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

    private TextView mTextConnectionStatus;
    private TextView mTextProduct;
    private Button mBtnOpen;
    private TextView sdkVersionTV;
    protected Button loginBtn;
    protected Button logoutBtn;
    protected TextView bindingStateTV;
    protected TextView appActivationStateTV;
    protected TextView loginStateTV;
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
    private static final int REQUEST_PERMISSION_CODE = 12345;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate: ConnectionActivity");

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
            showToast("test");
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
        ((MApplication)getApplicationContext()).demoApplication.setLoginStateTV_text(loginStateTV.getText().toString());
        ((MApplication)getApplicationContext()).demoApplication.setSdkVersionTV_text(sdkVersionTV.getText().toString());
        ((MApplication)getApplicationContext()).demoApplication.setmTextConnectionStatus_text(mTextConnectionStatus.getText().toString());
        ((MApplication)getApplicationContext()).demoApplication.setAppActivationStateTV_text(appActivationStateTV.getText().toString());
    }

    @Override
    public void onPause() {
        Log.e(TAG, "onPause");
        super.onPause();
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
        super.onDestroy();
    }

    private void initUI() {
        mTextConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        mTextProduct = (TextView) findViewById(R.id.text_product_info);
        mBtnOpen = (Button) findViewById(R.id.btn_open);
        mBtnOpen.setOnClickListener(this);
        mBtnOpen.setEnabled(false);
        sdkVersionTV = (TextView) findViewById(R.id.text_sdk_version);
        sdkVersionTV.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));
        bindingStateTV = (TextView) findViewById(R.id.tv_binding_state_info);
        appActivationStateTV = (TextView) findViewById(R.id.tv_activation_state_info);
        loginStateTV = (TextView) findViewById(R.id.login_state_info);
        loginBtn = (Button) findViewById(R.id.btn_login);
        logoutBtn = (Button) findViewById(R.id.btn_logout);
        loginBtn.setOnClickListener(this);
        logoutBtn.setOnClickListener(this);
    }

    private void initData() {
        setUpListener();
        appActivationManager = DJISDKManager.getInstance().getAppActivationManager();
        if (appActivationManager != null) {
            showToast("adding listeners");
            appActivationManager.addAppActivationStateListener(activationStateListener);
            appActivationManager.addAircraftBindingStateListener(bindingStateListener);
            appActivationStateTV.setText(appActivationManager.getAppActivationState().toString());
            bindingStateTV.setText(appActivationManager.getAircraftBindingState().toString());
        }
    }

    private void setUpListener() {
        // Example of Listener
        showToast("setting up listener");
        activationStateListener = new AppActivationState.AppActivationStateListener() {
            @Override
            public void onUpdate(final AppActivationState appActivationState) {
                ConnectionActivity.this.runOnUiThread(() -> appActivationStateTV.setText(appActivationState.toString()));
            }
        };
        bindingStateListener = bindingState -> ConnectionActivity.this.runOnUiThread(() -> bindingStateTV.setText(bindingState.toString()));
    }

    private void tearDownListener() {
        if (activationStateListener != null) {
            appActivationManager.removeAppActivationStateListener(activationStateListener);
            appActivationStateTV.setText(AppActivationState.UNKNOWN.toString());
        }
        if (bindingStateListener != null) {
            appActivationManager.removeAircraftBindingStateListener(bindingStateListener);
            bindingStateTV.setText(AircraftBindingState.UNKNOWN.toString());
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
            mBtnOpen.setEnabled(true);

            String str = mProduct instanceof Aircraft ? "无人机" : "手持云台";
            mTextConnectionStatus.setText(String.format("状态: 已连接%s", str));

            if (null != mProduct.getModel()) {
                mTextProduct.setText(mProduct.getModel().getDisplayName());
            } else {
                mTextProduct.setText(R.string.product_information);
            }
        } else {
            Log.v(TAG, "refreshSDK: False");
            mBtnOpen.setEnabled(false);

            mTextProduct.setText(R.string.product_information);
            mTextConnectionStatus.setText(R.string.connection_loose);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_open) {
            if (appActivationStateTV != null && bindingStateTV != null) {
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
                        showToast("登录成功");
                        UserAccountManager.getInstance().getLoggedInDJIUserAccountName(
                                new CommonCallbacks.CompletionCallbackWith<String>() {
                                    @Override
                                    public void onSuccess(final String username) {
                                        ConnectionActivity.this.runOnUiThread(() -> loginStateTV.setText(username));
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
                ConnectionActivity.this.runOnUiThread(() -> loginStateTV.setText("未登录"));
            } else {
                showToast("注销失败: " + error.getDescription());
            }
        });
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }
}
