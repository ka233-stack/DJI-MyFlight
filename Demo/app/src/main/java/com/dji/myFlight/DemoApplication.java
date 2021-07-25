package com.dji.myFlight;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.camera.Camera;
import dji.sdk.products.Aircraft;
import dji.sdk.products.HandHeld;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

public class DemoApplication extends Application {
    public static final String FLAG_CONNECTION_CHANGE = "demo_connection_change";
    private static final String TAG = ConnectionActivity.class.getName();

    private static BaseProduct mProduct;
    public Handler mHandler;

    private Application instance;

    public void setContext(Application application) {
        instance = application;
    }

    @Override
    public Context getApplicationContext() {
        return instance;
    }

    public DemoApplication() {

    }

    /**
     * This function is used to get the instance of DJIBaseProduct.
     * If no product is connected, it returns null.
     */
    public static synchronized BaseProduct getProductInstance() {
        if (null == mProduct) {
            mProduct = DJISDKManager.getInstance().getProduct();
        }
        return mProduct;
    }

    public static synchronized Camera getCameraInstance() {
        if (getProductInstance() == null) return null;
        Camera camera = null;
        if (getProductInstance() instanceof Aircraft){
            camera = ((Aircraft) getProductInstance()).getCamera();
        } else if (getProductInstance() instanceof HandHeld) {
            camera = ((HandHeld) getProductInstance()).getCamera();
        }
        return camera;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if(DJISDKManager.getInstance().hasSDKRegistered()){
            Toast.makeText(getApplicationContext(), "onCreate调用hasSDKRegistered结果为true(DemoApplication)", Toast.LENGTH_LONG).show();
        }
        else{
            Toast.makeText(getApplicationContext(), "onCreate调用hasSDKRegistered结果为false(DemoApplication)", Toast.LENGTH_LONG).show();
        }
        Log.d(TAG, "onCreate:DemoApplication");
        mHandler = new Handler(Looper.getMainLooper());

        /**
         * When starting SDK services, an instance of interface DJISDKManager.DJISDKManagerCallback will be used to listen to
         * the SDK Registration result and the product changing.
         */
        // 监听SDK的注册结果
        DJISDKManager.SDKManagerCallback mDJISDKManagerCallback = new DJISDKManager.SDKManagerCallback() {

            //Listens to the SDK registration result
            @Override
            public void onRegister(DJIError error) {
                Handler handler = new Handler(Looper.getMainLooper());
                if (error == DJISDKError.REGISTRATION_SUCCESS) {
                    handler.post(() -> Toast.makeText(getApplicationContext(), "注册成功(DemoApplication)", Toast.LENGTH_LONG).show());
                    DJISDKManager.getInstance().startConnectionToProduct();
                } else {
                    handler.post(() -> Toast.makeText(getApplicationContext(), "注册SDK失败，检查网络是否可用", Toast.LENGTH_LONG).show());
                }
                Log.e("TAG", error.toString());
            }

            @Override
            public void onProductDisconnect() {
                Log.d("TAG", "onProductDisconnect");
                notifyStatusChange();
            }

            @Override
            public void onProductConnect(BaseProduct baseProduct) {
                Log.d("TAG", String.format("onProductConnect newProduct:%s", baseProduct));
                notifyStatusChange();

            }

            @Override
            public void onProductChanged(BaseProduct baseProduct) {

            }

            @Override
            public void onComponentChange(BaseProduct.ComponentKey componentKey, BaseComponent oldComponent,
                                          BaseComponent newComponent) {
                if (newComponent != null) {
                    newComponent.setComponentListener(isConnected -> {
                        Log.d("TAG", "onComponentConnectivityChanged: " + isConnected);
                        notifyStatusChange();
                    });
                }

                Log.d("TAG",
                        String.format("onComponentChange key:%s, oldComponent:%s, newComponent:%s",
                                componentKey,
                                oldComponent,
                                newComponent));

            }

            @Override
            public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int i) {

            }

            @Override
            public void onDatabaseDownloadProgress(long l, long l1) {

            }
        };
        //Check the permissions before registering the application for android system 6.0 above.
        int permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int permissionCheck2 = ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.READ_PHONE_STATE);
        if (DJISDKManager.getInstance().hasSDKRegistered()) {
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || (permissionCheck == 0 && permissionCheck2 == 0)) {
            //This is used to start SDK services and initiate SDK.
            DJISDKManager.getInstance().registerApp(getApplicationContext(), mDJISDKManagerCallback);
            Toast.makeText(getApplicationContext(), "应用注册中(DemoApplication)", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), "请检查权限许可", Toast.LENGTH_LONG).show();
        }

    }

    private void notifyStatusChange() {
        mHandler.removeCallbacks(updateRunnable);
        mHandler.postDelayed(updateRunnable, 500);
    }

    private Runnable updateRunnable = () -> {
        Intent intent = new Intent(FLAG_CONNECTION_CHANGE);
        getApplicationContext().sendBroadcast(intent);
    };
}
