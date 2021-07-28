package com.dji.myFlight;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import dji.common.airlink.PhysicalSource;
import dji.common.product.Model;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.ux.beta.accessory.widget.rtk.RTKWidget;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.ux.beta.core.extension.ViewExtensions;
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget;
import dji.ux.beta.core.util.DisplayUtil;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.fpv.FPVWidget;
import dji.ux.beta.core.widget.gpssignal.GPSSignalWidget;
import dji.ux.beta.core.widget.radar.RadarWidget;
import dji.ux.beta.core.widget.simulator.SimulatorIndicatorWidget;
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget;
import dji.ux.beta.core.widget.useraccount.UserAccountLoginWidget;
import dji.ux.beta.map.widget.map.MapWidget;
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget;

public class ManualFlightActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mMediaManagerBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_flight);
        mMediaManagerBtn = (Button) findViewById(R.id.btn_mediaManager);
        mMediaManagerBtn.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_mediaManager) {
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);
        }
    }
}