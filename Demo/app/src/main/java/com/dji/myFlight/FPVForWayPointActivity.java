package com.dji.myFlight;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import dji.common.airlink.PhysicalSource;
import dji.common.product.Model;
import dji.thirdparty.io.reactivex.android.schedulers.AndroidSchedulers;
import dji.thirdparty.io.reactivex.disposables.CompositeDisposable;
import dji.ux.beta.accessory.widget.rtk.RTKWidget;
import dji.ux.beta.cameracore.widget.fpvinteraction.FPVInteractionWidget;
import dji.ux.beta.core.extension.ViewExtensions;
import dji.ux.beta.core.panel.systemstatus.SystemStatusListPanelWidget;
import dji.ux.beta.core.panel.topbar.TopBarPanelWidget;
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.fpv.FPVWidget;
import dji.ux.beta.core.widget.gpssignal.GPSSignalWidget;
import dji.ux.beta.core.widget.radar.RadarWidget;
import dji.ux.beta.core.widget.simulator.SimulatorIndicatorWidget;
import dji.ux.beta.core.widget.systemstatus.SystemStatusWidget;
import dji.ux.beta.map.widget.map.MapWidget;
import dji.ux.beta.training.widget.simulatorcontrol.SimulatorControlWidget;

public class FPVForWayPointActivity extends AppCompatActivity implements View.OnClickListener {

    //region Fields
    protected RadarWidget radarWidget;
    protected FPVWidget fpvWidget;
    protected FPVWidget secondaryFPVWidget;
    protected FPVInteractionWidget fpvInteractionWidget;
    protected MapWidget mapWidget;
    protected ConstraintLayout parentView;
    protected SystemStatusListPanelWidget systemStatusListPanelWidget;
    protected RTKWidget rtkWidget;
    protected SimulatorControlWidget simulatorControlWidget;
    private CompositeDisposable compositeDisposable;
    private Button btnGallery;
    //endregion

    //region Lifecycle
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_flight);

        initUI();
        setM200SeriesWarningLevelRanges();
        mapWidget.initAMap(map -> {
            map.setOnMapClickListener(latLng -> onViewClick(mapWidget));
            map.getUiSettings().setZoomControlsEnabled(false);
            map.getUiSettings().setScrollGesturesEnabled(false);
        });
        mapWidget.getUserAccountLoginWidget().setVisibility(View.GONE);
        mapWidget.onCreate(savedInstanceState);

        // ????????????????????????
        TopBarPanelWidget topBarPanel = findViewById(R.id.panel_top_bar);
        SystemStatusWidget systemStatusWidget = topBarPanel.getSystemStatusWidget();
        if (systemStatusWidget != null) {
            systemStatusWidget.setStateChangeCallback(findViewById(R.id.widget_panel_system_status_list));
        }

        SimulatorIndicatorWidget simulatorIndicatorWidget = topBarPanel.getSimulatorIndicatorWidget();
        if (simulatorIndicatorWidget != null) {
            simulatorIndicatorWidget.setStateChangeCallback(findViewById(R.id.widget_simulator_control));
        }

        GPSSignalWidget gpsSignalWidget = topBarPanel.getGPSSignalWidget();
        if (gpsSignalWidget != null) {
            gpsSignalWidget.setStateChangeCallback(findViewById(R.id.widget_rtk));
        }
    }

    private void initUI() {
        radarWidget = (RadarWidget) findViewById(R.id.widget_radar);
        fpvWidget = (FPVWidget) findViewById(R.id.widget_fpv);
        fpvInteractionWidget = (FPVInteractionWidget) findViewById(R.id.widget_fpv_interaction);
        mapWidget = (MapWidget) findViewById(R.id.widget_map);
        secondaryFPVWidget = (FPVWidget) findViewById(R.id.widget_secondary_fpv);
        parentView = (ConstraintLayout) findViewById(R.id.root_view);
        systemStatusListPanelWidget = (SystemStatusListPanelWidget) findViewById(R.id.widget_panel_system_status_list);
        // camera
        rtkWidget = (RTKWidget) findViewById(R.id.widget_rtk);
        simulatorControlWidget = (SimulatorControlWidget) findViewById(R.id.widget_simulator_control);

        btnGallery = (Button) findViewById(R.id.btn_mediaManager);

        secondaryFPVWidget.setOnClickListener(this);
        fpvWidget.setOnClickListener(this);
        btnGallery.setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        mapWidget.onDestroy();
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
        mapWidget.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapWidget.onLowMemory();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapWidget.onResume();
        compositeDisposable = new CompositeDisposable();
        compositeDisposable.add(secondaryFPVWidget.getCameraName()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(this::updateSecondaryVideoVisibility));

        compositeDisposable.add(systemStatusListPanelWidget.closeButtonPressed()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pressed -> {
                    if (pressed) {
                        ViewExtensions.hide(systemStatusListPanelWidget);
                    }
                }));

        compositeDisposable.add(rtkWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uiState -> {
                    if (uiState instanceof RTKWidget.UIState.VisibilityUpdated) {
                        if (((RTKWidget.UIState.VisibilityUpdated) uiState).isVisible()) {
                            hideOtherPanels(rtkWidget);
                        }
                    }
                }));
        compositeDisposable.add(simulatorControlWidget.getUIStateUpdates()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(simulatorControlWidgetState -> {
                    if (simulatorControlWidgetState instanceof SimulatorControlWidget.UIState.VisibilityUpdated) {
                        if (((SimulatorControlWidget.UIState.VisibilityUpdated) simulatorControlWidgetState).isVisible()) {
                            hideOtherPanels(simulatorControlWidget);
                        }
                    }
                }));
        fpvWidget.setVideoSource(SettingDefinitions.VideoSource.AUTO);
    }

    @Override
    protected void onPause() {
        if (compositeDisposable != null) {
            compositeDisposable.dispose();
            compositeDisposable = null;
        }
        mapWidget.onPause();
        super.onPause();
    }
    //endregion

    //region Utils

    private void hideOtherPanels(@Nullable View widget) {
        View[] panels = {
                rtkWidget,
                simulatorControlWidget
        };

        for (View panel : panels) {
            if (widget != panel) {
                panel.setVisibility(View.GONE);
            }
        }
    }

    private void setM200SeriesWarningLevelRanges() {
        Model[] m200SeriesModels = {
                Model.MATRICE_200,
                Model.MATRICE_210,
                Model.MATRICE_210_RTK,
                Model.MATRICE_200_V2,
                Model.MATRICE_210_V2,
                Model.MATRICE_210_RTK_V2
        };
        float[] ranges = {70, 30, 20, 12, 6, 3};
        radarWidget.setWarningLevelRanges(m200SeriesModels, ranges);
    }


    /**
     * ?????? FPV??? Map Widget
     *
     * @param view The thumbnail view that was clicked.
     */
    private void onViewClick(View view) {
        // Intent intent = new Intent(this, WaypointMissionActivity.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        // startActivity(intent);
        this.finish();
    }

    /**
     * ??????FPV???secondary FPV widget????????????
     */
    private void swapVideoSource() {
        if (secondaryFPVWidget.getVideoSource() == SettingDefinitions.VideoSource.SECONDARY) {
            fpvWidget.setVideoSource(SettingDefinitions.VideoSource.SECONDARY);
            secondaryFPVWidget.setVideoSource(SettingDefinitions.VideoSource.PRIMARY);
        } else {
            fpvWidget.setVideoSource(SettingDefinitions.VideoSource.PRIMARY);
            secondaryFPVWidget.setVideoSource(SettingDefinitions.VideoSource.SECONDARY);
        }
    }

    /**
     * Hide the secondary FPV widget when there is no secondary camera.
     *
     * @param cameraName The name of the secondary camera.
     */
    private void updateSecondaryVideoVisibility(String cameraName) {
        if (cameraName.equals(PhysicalSource.UNKNOWN.name())) {
            secondaryFPVWidget.setVisibility(View.GONE);
        } else {
            secondaryFPVWidget.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.widget_secondary_fpv) {
            swapVideoSource();
        } else if (id == R.id.widget_fpv) {
            onViewClick(fpvWidget);
        } else if (id == R.id.btn_mediaManager) {
            Intent intent = new Intent(this, GalleryActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }
}