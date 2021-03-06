package com.dji.myFlight;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.common.realname.AircraftBindingState;
import dji.common.realname.AppActivationState;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.log.DJILog;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.realname.AppActivationManager;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class ConnectionActivity extends Activity implements View.OnClickListener {

    private static final String TAG = ConnectionActivity.class.getName();
    private static final int REQUEST_PERMISSION_CODE = 12345;

    private static boolean isAppStarted = false;

    private Button btnOpen;
    private Button btnLogin;
    private Button btnLogout;
    protected TextView tvConnectionStatus;
    protected TextView tvProductInfo;
    protected TextView tvBindingState;
    protected TextView tvAppActivationState;
    protected TextView tvUsername;
    protected TextView tvSDKVersion;
    // 添加侦听器获取应用程序激活状态和飞行器绑定状态
    private AppActivationManager appActivationManager;
    private AppActivationState.AppActivationStateListener activationStateListener;
    private AircraftBindingState.AircraftBindingStateListener bindingStateListener;

    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.VIBRATE,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
    };

    private List<String> missingPermission = new ArrayList<>();
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isAppStarted = true;
        checkAndRequestPermissions();
        setContentView(R.layout.activity_connection);

        initUI();

        // 注册广播接收器以接收设备连接的变化
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // 检查权限
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // 申请缺失的权限
        if (!missingPermission.isEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[0]),
                    REQUEST_PERMISSION_CODE);
        }

    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check for granted permission and remove from missing list
        if (requestCode == REQUEST_PERMISSION_CODE) {
            for (int i = grantResults.length - 1; i >= 0; i--) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    missingPermission.remove(permissions[i]);
                }
            }
        }
        // If there is enough permission, we will start the registration
        if (!missingPermission.isEmpty()) {
            showToast("缺少权限");
        } else if (!DJISDKManager.getInstance().hasSDKRegistered()) {
            startSDKRegistration();
        } else {

        }
    }

    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> {
                DJISDKManager.getInstance().registerApp(getApplicationContext(), new DJISDKManager.SDKManagerCallback() {
                    @Override
                    public void onRegister(DJIError djiError) {
                        if (djiError == DJISDKError.REGISTRATION_SUCCESS) {
                            DJILog.e("App registration", DJISDKError.REGISTRATION_SUCCESS.getDescription());
                            DJISDKManager.getInstance().startConnectionToProduct();
                            showToast("注册成功(ConnectionActivity)");
                        } else {
                            showToast("注册SDK失败，检查网络是否可用");
                        }
                        Log.v(TAG, djiError.getDescription());
                    }

                    @Override
                    public void onProductDisconnect() {
                        Log.d(TAG, "onProductDisconnect");
                        showToast("产品已断开连接");

                    }

                    @Override
                    public void onProductConnect(BaseProduct baseProduct) {
                        Log.d(TAG, String.format("onProductConnect newProduct:%s", baseProduct));
                        showToast("产品已连接");

                    }

                    @Override
                    public void onProductChanged(BaseProduct baseProduct) {

                    }

                    @Override
                    public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                                  BaseComponent newComponent) {
                        if (newComponent != null) {
                            newComponent.setComponentListener(isConnected -> Log.d(TAG, "onComponentConnectivityChanged: " + isConnected));
                        }
                        Log.d(TAG, String.format("onComponentChange key:%s, " + "oldComponent:%s, " + "newComponent:%s",
                                componentKey, oldComponent, newComponent));
                    }

                    @Override
                    public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

                    }

                    @Override
                    public void onDatabaseDownloadProgress(long l, long l1) {

                    }
                });
            });
        }
    }

    @Override
    public void onResume() {
        Log.e(TAG, "onResume");
        super.onResume();
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
        ((MApplication) getApplicationContext()).demoApplication.setLoginState(tvUsername.getText().toString());
        ((MApplication) getApplicationContext()).demoApplication.setSdkVersion(tvSDKVersion.getText().toString());
        ((MApplication) getApplicationContext()).demoApplication.setConnectionStatus(tvConnectionStatus.getText().toString());
        ((MApplication) getApplicationContext()).demoApplication.setAppActivationState(tvAppActivationState.getText().toString());
    }

    @Override
    public void onStop() {
        Log.e(TAG, "onStop");
        super.onStop();
    }

    public void onReturn(View view) {
        Log.e(TAG, "onReturn");
        this.finish();
    }

    @Override
    protected void onDestroy() {
        Log.e(TAG, "onDestroy");
        unregisterReceiver(mReceiver);
        tearDownListener();
        isAppStarted = false;
        super.onDestroy();
    }

    private void initUI() {
        tvConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        tvProductInfo = (TextView) findViewById(R.id.text_product_info);
        btnOpen = (Button) findViewById(R.id.btn_open);
        tvSDKVersion = (TextView) findViewById(R.id.text_sdk_version);
        tvBindingState = (TextView) findViewById(R.id.tv_binding_state_info);
        tvAppActivationState = (TextView) findViewById(R.id.tv_activation_state_info);
        tvUsername = (TextView) findViewById(R.id.login_state_info);
        btnLogin = (Button) findViewById(R.id.btn_login);
        btnLogout = (Button) findViewById(R.id.btn_logout);

        btnOpen.setEnabled(false);
        tvSDKVersion.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        btnOpen.setOnClickListener(this);
        btnLogin.setOnClickListener(this);
        btnLogout.setOnClickListener(this);
    }

    private void initData() {
        setUpListener();
        appActivationManager = DJISDKManager.getInstance().getAppActivationManager();
        if (appActivationManager != null) {
            appActivationManager.addAppActivationStateListener(activationStateListener);
            appActivationManager.addAircraftBindingStateListener(bindingStateListener);
            tvAppActivationState.setText(appActivationManager.getAppActivationState().toString());
            tvBindingState.setText(appActivationManager.getAircraftBindingState().toString());
        }
    }

    private void setUpListener() {
        // Example of Listener
        activationStateListener = appActivationState -> ConnectionActivity.this.runOnUiThread(
                () -> tvAppActivationState.setText(appActivationState.toString()));
        bindingStateListener = bindingState -> ConnectionActivity.this.runOnUiThread(
                () -> tvBindingState.setText(bindingState.toString()));
    }

    private void tearDownListener() {
        if (activationStateListener != null) {
            appActivationManager.removeAppActivationStateListener(activationStateListener);
            tvAppActivationState.setText(AppActivationState.UNKNOWN.toString());
        }
        if (bindingStateListener != null) {
            appActivationManager.removeAircraftBindingStateListener(bindingStateListener);
            tvBindingState.setText(AircraftBindingState.UNKNOWN.toString());
        }
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            refreshSDKRelativeUI();
            initData();
        }
    };

    private void refreshSDKRelativeUI() {
        BaseProduct mProduct = DemoApplication.getProductInstance();

        if (null != mProduct && mProduct.isConnected()) {
            Log.v(TAG, "refreshSDK: True");
            btnOpen.setEnabled(true);

            String str = mProduct instanceof Aircraft ? "无人机" : "手持云台";
            tvConnectionStatus.setText(String.format("状态: 已连接%s", str));

            if (null != mProduct.getModel()) {
                tvProductInfo.setText(mProduct.getModel().getDisplayName());
            } else {
                tvProductInfo.setText(R.string.product_info);
            }
        } else {
            Log.v(TAG, "refreshSDK: False");
            btnOpen.setEnabled(false);

            tvProductInfo.setText(R.string.product_info);
            tvConnectionStatus.setText(R.string.connection_loose);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_open) {
            if (tvAppActivationState != null && tvBindingState != null) {
                Intent intent = new Intent(this, DashboardActivity.class);
                startActivity(intent);
            }
        } else if (id == R.id.btn_login) {
            loginAccount();
        } else if (id == R.id.btn_logout) {
            logoutAccount();
        }
    }

    private void loginAccount() {
        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        UserAccountManager.getInstance().getLoggedInDJIUserAccountName(
                                new CommonCallbacks.CompletionCallbackWith<String>() {
                                    @Override
                                    public void onSuccess(final String username) {
                                        ConnectionActivity.this.runOnUiThread(() -> tvUsername.setText(username));
                                    }

                                    @Override
                                    public void onFailure(DJIError error) {
                                        showToast("获取用户名失败: " + error.getDescription());
                                    }
                                });
                    }

                    @Override
                    public void onFailure(DJIError error) {
                        showToast("登录失败: " + error.getDescription());
                    }
                });
    }

    private void logoutAccount() {
        UserAccountManager.getInstance().logoutOfDJIUserAccount(error -> {
            if (null == error) {
                showToast("已注销登录");
                ConnectionActivity.this.runOnUiThread(() -> tvUsername.setText("未登录"));
            } else {
                showToast("注销失败: " + error.getDescription());
            }
        });
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }

    /**
     * Whether the app has started.
     *
     * @return `true` if the app has been started.
     */
    public static boolean isStarted() {
        return isAppStarted;
    }
}
