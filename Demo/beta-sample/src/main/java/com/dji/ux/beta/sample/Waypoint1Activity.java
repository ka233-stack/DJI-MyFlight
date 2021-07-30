package com.dji.ux.beta.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.AMap.OnMapClickListener;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.useraccount.UserAccountState;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;
import dji.sdk.useraccount.UserAccountManager;

public class Waypoint1Activity extends FragmentActivity implements View.OnClickListener, OnMapClickListener{

    protected static final String TAG = "Waypoint1Activity";

    private MapView mapView;
    private AMap aMap;



    private EditText editText_v,editText_v1;
    private Button btn_commit;
    private Button btn_addPoint_mode,btn_clearPoint;
    private EditText speed_edittext,altitude_edittext;
    private Spinner action_spinner,angle_spinner;
    private Button btn_upload,btn_start,btn_stop;



    private Button locate, add, clear;
    private Button config, upload, start, stop;

    private boolean isAdd = false;

    private double droneLocationLat = 181, droneLocationLng = 181;
    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<Integer, Marker>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    @Override
    protected void onResume(){
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onDestroy(){
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view){
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string){
        Waypoint1Activity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Waypoint1Activity.this, string, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initUI() {
        editText_v=(EditText)findViewById(R.id.point_v);
        editText_v1=(EditText)findViewById(R.id.point_v1);
        btn_commit=(Button) findViewById(R.id.btn_commit);
        btn_addPoint_mode=(Button) findViewById(R.id.btn_addPoint_mode);
        btn_clearPoint=(Button) findViewById(R.id.btn_clearPoint);
        speed_edittext=(EditText)findViewById(R.id.speed_edittxet);
        altitude_edittext=(EditText)findViewById(R.id.altitude_edittext);
        action_spinner=(Spinner)findViewById(R.id.action_spinner);
        angle_spinner=(Spinner)findViewById(R.id.angle_spinner);
        btn_upload=(Button) findViewById(R.id.btn_upload);
        btn_start=(Button) findViewById(R.id.btn_start);
        btn_stop=(Button) findViewById(R.id.btn_stop);

        btn_commit.setOnClickListener(this);
        btn_addPoint_mode.setOnClickListener(this);
        btn_clearPoint.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);

    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        LatLng shenzhen = new LatLng(22.5362, 113.9454);
        aMap.addMarker(new MarkerOptions().position(shenzhen).title("Marker in Shenzhen"));
        aMap.moveCamera(CameraUpdateFactory.newLatLng(shenzhen));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.test);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DJIDemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        initMapView();
        initUI();
        addListener();
        onProductConnectionChange();
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            onProductConnectionChange();
        }
    };

    private void onProductConnectionChange()
    {
        initFlightController();
        loginAccount();
    }

    private void loginAccount(){

        UserAccountManager.getInstance().logIntoDJIUserAccount(this,
                new CommonCallbacks.CompletionCallbackWith<UserAccountState>() {
                    @Override
                    public void onSuccess(final UserAccountState userAccountState) {
                        Log.e(TAG, "Login Success");
                    }
                    @Override
                    public void onFailure(DJIError error) {
                        setResultToToast("Login Error:"
                                + error.getDescription());
                    }
                });
    }

    private void initFlightController() {

        BaseProduct product = DJIDemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {

            mFlightController.setStateCallback(
                    new FlightControllerState.Callback() {
                        @Override
                        public void onUpdate(FlightControllerState
                                                     djiFlightControllerCurrentState) {
                            droneLocationLat = djiFlightControllerCurrentState.getAircraftLocation().getLatitude();
                            droneLocationLng = djiFlightControllerCurrentState.getAircraftLocation().getLongitude();
                            updateDroneLocation();
                        }
                    });

        }
    }

    //Add Listener for WaypointMissionOperator
    private void addListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().addListener(eventNotificationListener);
        }
    }

    private void removeListener() {
        if (getWaypointMissionOperator() != null) {
            getWaypointMissionOperator().removeListener(eventNotificationListener);
        }
    }

    private WaypointMissionOperatorListener eventNotificationListener = new WaypointMissionOperatorListener() {
        @Override
        public void onDownloadUpdate(WaypointMissionDownloadEvent downloadEvent) {

        }

        @Override
        public void onUploadUpdate(WaypointMissionUploadEvent uploadEvent) {

        }

        @Override
        public void onExecutionUpdate(WaypointMissionExecutionEvent executionEvent) {

        }

        @Override
        public void onExecutionStart() {

        }

        @Override
        public void onExecutionFinish(@Nullable final DJIError error) {
            setResultToToast("Execution finished: " + (error == null ? "Success!" : error.getDescription()));
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd){
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }else{
            setResultToToast("Cannot Add Waypoint");
        }
    }

    public static boolean checkGpsCoordination(double latitude, double longitude) {
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    // Update the drone location based on states from MCU.
    private void updateDroneLocation(){

        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        //Create MarkerOptions object
        final MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(pos);
        markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (droneMarker != null) {
                    droneMarker.remove();
                }

                if (checkGpsCoordination(droneLocationLat, droneLocationLng)) {
                    droneMarker = aMap.addMarker(markerOptions);
                }
            }
        });
    }

    private void markWaypoint(LatLng point){
        //Create MarkerOptions object
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(point);
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        Marker marker = aMap.addMarker(markerOptions);
        mMarkers.put(mMarkers.size(), marker);
    }

    private void addPointByLonLat(){
        double point_v =  Double.parseDouble(editText_v.getText().toString());
        double point_v1 =  Double.parseDouble(editText_v1.getText().toString());
        showToast(editText_v.toString());
        showToast(editText_v1.toString());
        if(editText_v.getText().equals("") || editText_v.getText()==null || editText_v1.getText().equals("") || editText_v1.getText()==null)
            showToast("Please Enter First");
        else{
            LatLng point=new LatLng(point_v,point_v1);
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }else
            {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_addPoint_mode:{
                enableDisableAdd();
                break;
            }
            case R.id.btn_clearPoint: {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        aMap.clear();
                    }

                });
                waypointList.clear();
                waypointMissionBuilder.waypointList(waypointList);
                updateDroneLocation();
                break;
            }
            case R.id.btn_upload:{
                set_settings();
                if(!checkConditions()){
                    showToast("请检查输入的参数");
                }
                else {
                    configWayPointMission();
                    uploadWayPointMission();
                }
                break;
            }
            case R.id.btn_start:{
                startWaypointMission();
                break;
            }
            case R.id.btn_stop:{
                stopWaypointMission();
                break;
            }
            case R.id.btn_commit:{
                addPointByLonLat();
                break;
            }
            default:
                break;
        }
    }

    private void cameraUpdate(){
        LatLng pos = new LatLng(droneLocationLat, droneLocationLng);
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(pos, zoomlevel);
        aMap.moveCamera(cu);
    }

    private void enableDisableAdd(){
        if (isAdd == false) {
            isAdd = true;
            btn_addPoint_mode.setText("Exit");
        }else{
            isAdd = false;
            btn_addPoint_mode.setText("Add");
        }
    }

//    private void showSettingDialog(){
//        LinearLayout wayPointSettings = (LinearLayout)getLayoutInflater().inflate(R.layout.dialog_waypointsetting, null);
//
//        final TextView wpAltitude_TV = (TextView) wayPointSettings.findViewById(R.id.altitude);
//        RadioGroup speed_RG = (RadioGroup) wayPointSettings.findViewById(R.id.speed);
//        RadioGroup actionAfterFinished_RG = (RadioGroup) wayPointSettings.findViewById(R.id.actionAfterFinished);
//        RadioGroup heading_RG = (RadioGroup) wayPointSettings.findViewById(R.id.heading);
//
//        speed_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
//
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                if (checkedId == R.id.lowSpeed){
//                    mSpeed = 3.0f;
//                } else if (checkedId == R.id.MidSpeed){
//                    mSpeed = 5.0f;
//                } else if (checkedId == R.id.HighSpeed){
//                    mSpeed = 10.0f;
//                }
//            }
//
//        });
//
//        actionAfterFinished_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                Log.d(TAG, "Select finish action");
//                if (checkedId == R.id.finishNone){
//                    mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
//                } else if (checkedId == R.id.finishGoHome){
//                    mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
//                } else if (checkedId == R.id.finishAutoLanding){
//                    mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
//                } else if (checkedId == R.id.finishToFirst){
//                    mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
//                }
//            }
//        });
//
//        heading_RG.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                Log.d(TAG, "Select heading");
//
//                if (checkedId == R.id.headingNext) {
//                    mHeadingMode = WaypointMissionHeadingMode.AUTO;
//                } else if (checkedId == R.id.headingInitDirec) {
//                    mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
//                } else if (checkedId == R.id.headingRC) {
//                    mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
//                } else if (checkedId == R.id.headingWP) {
//                    mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
//                }
//            }
//        });
//
//        new AlertDialog.Builder(this)
//                .setTitle("")
//                .setView(wayPointSettings)
//                .setPositiveButton("Finish",new DialogInterface.OnClickListener(){
//                    public void onClick(DialogInterface dialog, int id) {
//
//                        String altitudeString = wpAltitude_TV.getText().toString();
//                        altitude = Integer.parseInt(nulltoIntegerDefalt(altitudeString));
//                        Log.e(TAG,"altitude "+altitude);
//                        Log.e(TAG,"speed "+mSpeed);
//                        Log.e(TAG, "mFinishedAction "+mFinishedAction);
//                        Log.e(TAG, "mHeadingMode "+mHeadingMode);
//                        configWayPointMission();
//                    }
//
//                })
//                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int id) {
//                        dialog.cancel();
//                    }
//
//                })
//                .create()
//                .show();
//    }

    String nulltoIntegerDefalt(String value){
        if(!isIntValue(value)) value="0";
        return value;
    }

    boolean isIntValue(String val)
    {
        try {
            val=val.replace(" ","");
            Integer.parseInt(val);
        } catch (Exception e) {return false;}
        return true;
    }

    private void configWayPointMission(){

        if (waypointMissionBuilder == null){

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                                                                  .headingMode(mHeadingMode)
                                                                  .autoFlightSpeed(mSpeed)
                                                                  .maxFlightSpeed(mSpeed)
                                                                  .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }else
        {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0){

            for (int i=0; i< waypointMissionBuilder.getWaypointList().size(); i++){
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }

            setResultToToast("Set Waypoint attitude successfully");
        }

        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("loadWaypoint succeeded");
        } else {
            setResultToToast("loadWaypoint failed " + error.getDescription());
        }

    }

    private void uploadWayPointMission(){

        getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    setResultToToast("Mission upload successfully!");
                } else {
                    setResultToToast("Mission upload failed, error: " + error.getDescription() + " retrying...");
                    getWaypointMissionOperator().retryUploadMission(null);
                }
            }
        });

    }

    private void startWaypointMission(){

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission(){

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void showToast(final String toastMsg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void set_settings(){
        this.mSpeed=Float.parseFloat(speed_edittext.getText().toString());
        this.altitude=Float.parseFloat(altitude_edittext.getText().toString());
        String action,angle;
        action=action_spinner.getSelectedItem().toString();
        angle=angle_spinner.getSelectedItem().toString();
        switch(action){
            case "无动作":
                mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                break;
            case "自动返航":
                mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                break;
            case "自动着陆":
                mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                break;
            case "返回航线起始点":
                mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                break;
            default:
                showToast("完成动作有误");
                break;
        }
        switch(angle){
            case "headingNext":
                mHeadingMode = WaypointMissionHeadingMode.AUTO;
                break;
            case "headingInitDirec":
                mHeadingMode = WaypointMissionHeadingMode.USING_INITIAL_DIRECTION;
                break;
            case "headingRC":
                mHeadingMode = WaypointMissionHeadingMode.CONTROL_BY_REMOTE_CONTROLLER;
                break;
            case "headingWP":
                mHeadingMode = WaypointMissionHeadingMode.USING_WAYPOINT_HEADING;
                break;
            default:
                showToast("飞行器偏航角有误");
                break;
        }
    }

    private boolean checkConditions(){
        boolean flag=true;
        if((mSpeed<-15) || (mSpeed>15)){
            showToast("飞行速度要介于-15到15之间，请重新输入");
            flag=false;
        }
        if(altitude>120){
            showToast("飞行高度不可过高");
            flag=false;
        }
        return flag;
    }
}
