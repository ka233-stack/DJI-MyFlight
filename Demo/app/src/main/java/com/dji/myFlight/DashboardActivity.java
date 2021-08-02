package com.dji.myFlight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import dji.sdk.sdkmanager.DJISDKManager;

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvConnectionStatus;
    private TextView tvProductInfo;
    private ImageButton imgBtnControlManual;
    private ImageButton imgBtnControlRoutes;
    private TextView tvSDKVersion;
    protected ImageButton imgBtnGallery;
    protected TextView tvLoginState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        initUI();
        tvLoginState.setText(((MApplication) getApplicationContext()).demoApplication.getLoginState());
        tvSDKVersion.setText(((MApplication) getApplicationContext()).demoApplication.getSdkVersion());
        tvConnectionStatus.setText(((MApplication) getApplicationContext()).demoApplication.getConnectionStatus());
    }

    private void initUI() {
        tvConnectionStatus = (TextView) findViewById(R.id.text_connection_status);
        tvProductInfo = (TextView) findViewById(R.id.text_product_info);
        imgBtnControlManual = (ImageButton) findViewById(R.id.imgBtn_control_manual);
        imgBtnControlRoutes = (ImageButton) findViewById(R.id.imgBtn_control_routes);
        imgBtnControlManual.setOnClickListener(this);
        imgBtnControlRoutes.setOnClickListener(this);
        tvSDKVersion = (TextView) findViewById(R.id.text_sdk_version);
        tvSDKVersion.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));
        tvLoginState = (TextView) findViewById(R.id.login_state_info);
        imgBtnGallery = (ImageButton) findViewById(R.id.imgBtn_gallery);
        imgBtnGallery.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.imgBtn_control_manual) {
            imgBtnControlManual.setEnabled(false);
            Intent intent = new Intent(this, ManualFlightActivity.class);
            startActivity(intent);
        } else if (id == R.id.imgBtn_control_routes) {
            Intent intent = new Intent(this, WaypointMissionActivity.class);
            startActivity(intent);
        } else if (id == R.id.imgBtn_gallery) {
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        imgBtnControlManual.setEnabled(true);
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }
}