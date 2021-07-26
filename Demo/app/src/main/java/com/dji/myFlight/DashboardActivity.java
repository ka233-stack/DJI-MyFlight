package com.dji.myFlight;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
        tvLoginState.setText(((MApplication)getApplicationContext()).demoApplication.getLoginStateTV_text());
        tvSDKVersion.setText(((MApplication)getApplicationContext()).demoApplication.getSdkVersionTV_text());
        tvConnectionStatus.setText(((MApplication)getApplicationContext()).demoApplication.getmTextConnectionStatus_text());
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
            showToast("手动飞行");
        } else if (id == R.id.imgBtn_control_routes) {
            showToast("航线飞行");
        } else if(id == R.id.imgBtn_gallery){
            showToast("图库");
        }
    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }
}