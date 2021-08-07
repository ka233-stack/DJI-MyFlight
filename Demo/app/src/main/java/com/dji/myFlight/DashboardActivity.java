package com.dji.myFlight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import dji.sdk.sdkmanager.DJISDKManager;

public class DashboardActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView tvConnectionStatus;
    private TextView tvProductInfo;
    private TextView tvSDKVersion;
    private TextView tvLoginState;
    private ImageButton imgBtnControlManual;
    private ImageButton imgBtnControlRoutes;
    private ImageButton imgBtnGallery;
    private Button btnTest;


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
        tvLoginState = (TextView) findViewById(R.id.login_state_info);
        tvSDKVersion = (TextView) findViewById(R.id.text_sdk_version);
        imgBtnControlManual = (ImageButton) findViewById(R.id.imgBtn_control_manual);
        imgBtnControlRoutes = (ImageButton) findViewById(R.id.imgBtn_control_routes);
        imgBtnGallery = (ImageButton) findViewById(R.id.imgBtn_gallery);
        btnTest = (Button) findViewById(R.id.btn_test);

        tvSDKVersion.setText(getResources().getString(R.string.sdk_version, DJISDKManager.getInstance().getSDKVersion()));

        imgBtnControlManual.setOnClickListener(this);
        imgBtnControlRoutes.setOnClickListener(this);
        imgBtnGallery.setOnClickListener(this);
        btnTest.setOnClickListener(this);
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
        } else if (id == R.id.btn_test) {
            Intent intent = new Intent(this, WaypointMissionV2Activity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        imgBtnControlManual.setEnabled(true);
    }

}