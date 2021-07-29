package com.dji.myFlight;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dji.myFlight.R;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.error.DJIError;
import dji.common.error.DJISDKError;
import dji.sdk.base.BaseComponent;
import dji.sdk.base.BaseProduct;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKInitEvent;
import dji.sdk.sdkmanager.DJISDKManager;

/**
 * Handles the connection to the product and provides links to the different test activities. Also
 * shows the current connection state and displays logs for the different steps of the SDK
 * registration process.
 */
public class MainActivity extends AppCompatActivity {

    //region Constants
    private static final String LAST_USED_BRIDGE_IP = "bridgeip";
    private static final int REQUEST_PERMISSION_CODE = 12345;
    private static final String[] REQUIRED_PERMISSION_LIST = new String[]{
            Manifest.permission.VIBRATE, // Gimbal rotation
            Manifest.permission.INTERNET, // API requests
            Manifest.permission.ACCESS_WIFI_STATE, // WIFI connected products
            Manifest.permission.ACCESS_COARSE_LOCATION, // Maps
            Manifest.permission.ACCESS_NETWORK_STATE, // WIFI connected products
            Manifest.permission.ACCESS_FINE_LOCATION, // Maps
            Manifest.permission.CHANGE_WIFI_STATE, // Changing between WIFI and USB connection
            Manifest.permission.WRITE_EXTERNAL_STORAGE, // Log files
            Manifest.permission.BLUETOOTH, // Bluetooth connected products
            Manifest.permission.BLUETOOTH_ADMIN, // Bluetooth connected products
            Manifest.permission.READ_EXTERNAL_STORAGE, // Log files
            Manifest.permission.READ_PHONE_STATE, // Device UUID accessed upon registration
            Manifest.permission.RECORD_AUDIO // Speaker accessory
    };
    private static final String TIME_FORMAT = "MMM dd, yyyy 'at' h:mm:ss a";
    private static final String TAG = "MainActivity";
    //endregion
    private static boolean isAppStarted = false;
    @BindView(R.id.text_view_version)
    protected TextView versionTextView;
    @BindView(R.id.text_view_registered)
    protected TextView registeredTextView;
    @BindView(R.id.text_view_product_name)
    protected TextView productNameTextView;
    @BindView(R.id.edit_text_bridge_ip)
    protected EditText bridgeModeEditText;

    //region Fields
    private AtomicBoolean isRegistrationInProgress = new AtomicBoolean(false);
    private int lastProgress = -1;
    private DJISDKManager.SDKManagerCallback registrationCallback = new DJISDKManager.SDKManagerCallback() {

        @Override
        public void onRegister(DJIError error) {
            isRegistrationInProgress.set(false);
            if (error == DJISDKError.REGISTRATION_SUCCESS) {
                DJISDKManager.getInstance().startConnectionToProduct();
                runOnUiThread(() -> {
                    registeredTextView.setText(R.string.registered);
                });
            } else {
            }
        }

        @Override
        public void onProductDisconnect() {
            runOnUiThread(() -> {
                productNameTextView.setText(R.string.no_product);
            });
        }

        @Override
        public void onProductConnect(BaseProduct product) {
            if (product != null) {
                runOnUiThread(() -> {
                    if (product.getModel() != null) {
                        productNameTextView.setText(getString(R.string.product_name, product.getModel().getDisplayName()));
                    } else if (product instanceof Aircraft) {
                        Aircraft aircraft = (Aircraft) product;
                        if (aircraft.getRemoteController() != null) {
                            productNameTextView.setText(getString(R.string.remote_controller));
                        }
                    }
                });
            }
        }

        @Override
        public void onProductChanged(BaseProduct product) {
            if (product != null) {
                runOnUiThread(() -> {
                    if (product.getModel() != null) {
                        productNameTextView.setText(getString(R.string.product_name, product.getModel().getDisplayName()));
                    } else if (product instanceof Aircraft) {
                        Aircraft aircraft = (Aircraft) product;
                        if (aircraft.getRemoteController() != null) {
                            productNameTextView.setText(getString(R.string.remote_controller));
                        }
                    }
                });
            }
        }

        @Override
        public void onComponentChange(BaseProduct.ComponentKey key,
                                      BaseComponent oldComponent,
                                      BaseComponent newComponent) {
        }

        @Override
        public void onInitProcess(DJISDKInitEvent djisdkInitEvent, int totalProcess) {
        }

        @Override
        public void onDatabaseDownloadProgress(long current, long total) {
            runOnUiThread(() -> {
                int progress = (int) (100 * current / total);
                if (progress == lastProgress) {
                    return;
                }
                lastProgress = progress;
            });
        }
    };
    private List<String> missingPermission = new ArrayList<>();
    //endregion

    /**
     * Whether the app has started.
     *
     * @return `true` if the app has been started.
     */
    public static boolean isStarted() {
        return isAppStarted;
    }

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        isAppStarted = true;
        checkAndRequestPermissions();
        setBridgeModeEditText();
        versionTextView.setText(getResources().getString(R.string.sdk_version,
                DJISDKManager.getInstance().getSDKVersion()));
    }
    //endregion

    @Override
    protected void onDestroy() {
        // Prevent memory leak by releasing DJISDKManager's references to this activity
        if (DJISDKManager.getInstance() != null) {
            DJISDKManager.getInstance().destroy();
        }
        isAppStarted = false;
        super.onDestroy();
    }

    /**
     * Checks if there is any missing permissions, and
     * requests runtime permission if needed.
     */
    private void checkAndRequestPermissions() {
        // Check for permissions
        for (String eachPermission : REQUIRED_PERMISSION_LIST) {
            if (ContextCompat.checkSelfPermission(this, eachPermission) != PackageManager.PERMISSION_GRANTED) {
                missingPermission.add(eachPermission);
            }
        }
        // Request for missing permissions
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
            ActivityCompat.requestPermissions(this,
                    missingPermission.toArray(new String[missingPermission.size()]),
                    REQUEST_PERMISSION_CODE);
        }
    }

    /**
     * Result of runtime permission request
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
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
        if (missingPermission.isEmpty()) {
            startSDKRegistration();
        } else {
        }
    }

    /**
     * Start the SDK registration
     */
    private void startSDKRegistration() {
        if (isRegistrationInProgress.compareAndSet(false, true)) {
            AsyncTask.execute(() -> DJISDKManager.getInstance().registerApp(MainActivity.this, registrationCallback));
        }
    }

    /**
     * Initialize the bridge mode edit text
     */
    private void setBridgeModeEditText() {
        bridgeModeEditText.setText(PreferenceManager.getDefaultSharedPreferences(this)
                .getString(LAST_USED_BRIDGE_IP, ""));
        bridgeModeEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || event != null
                    && event.getAction() == KeyEvent.ACTION_DOWN
                    && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                if (event != null && event.isShiftPressed()) {
                    return false;
                } else {
                }
            }
            return false; // pass on to other listeners.
        });
        bridgeModeEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Nothing to do
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Nothing to do
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s != null && s.toString().contains("\n")) {
                    // the user is done typing.
                    // remove new line character
                    final String currentText = bridgeModeEditText.getText().toString();
                    bridgeModeEditText.setText(currentText.substring(0, currentText.indexOf('\n')));
                }
            }
        });
    }


    @OnClick(R.id.default_layout_button)
    public void onDefaultLayoutClick() {
        Intent intent = new Intent(this, DefaultLayoutActivity.class);
        startActivity(intent);
    }

}
