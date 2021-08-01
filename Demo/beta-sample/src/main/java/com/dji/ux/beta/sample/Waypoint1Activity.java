package com.dji.ux.beta.sample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
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
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dji.common.error.DJIError;
import dji.common.flightcontroller.FlightControllerState;
import dji.common.flightcontroller.LocationCoordinate3D;
import dji.common.mission.waypoint.Waypoint;
import dji.common.mission.waypoint.WaypointMission;
import dji.common.mission.waypoint.WaypointMissionDownloadEvent;
import dji.common.mission.waypoint.WaypointMissionExecutionEvent;
import dji.common.mission.waypoint.WaypointMissionFinishedAction;
import dji.common.mission.waypoint.WaypointMissionFlightPathMode;
import dji.common.mission.waypoint.WaypointMissionHeadingMode;
import dji.common.mission.waypoint.WaypointMissionUploadEvent;
import dji.common.util.CommonCallbacks;
import dji.sdk.base.BaseProduct;
import dji.sdk.flightcontroller.FlightController;
import dji.sdk.mission.waypoint.WaypointMissionOperator;
import dji.sdk.mission.waypoint.WaypointMissionOperatorListener;
import dji.sdk.products.Aircraft;
import dji.sdk.sdkmanager.DJISDKManager;

public class Waypoint1Activity extends FragmentActivity implements View.OnClickListener, OnMapClickListener {

    protected static final String TAG = "Waypoint1Activity";

    private static final int TODO_LINE = 0;
    private static final int FINISHED_LINE = 1;

    private MapView mapView;
    private AMap aMap;

    private EditText editText_v, editText_v1;
    private Button btn_commit;
    private Button btn_addPoint_mode, btn_clearPoint;
    private EditText speed_edittext, altitude_edittext;
    private Spinner action_spinner, angle_spinner;
    private Button btn_upload, btn_start, btn_stop;

    // 上一个航点坐标点
    private LatLng lastPoint = null;
    // 上一个飞机位置
    private LatLng lastDronePos = null;
    // 当前飞机位置
    private LatLng curDronePos = null;

    // 飞机已飞行轨迹列表
    private List<Polyline> finishedLineList = new ArrayList<>();
    // 航点轨迹列表
    private List<Polyline> todoLineList = new ArrayList<>();

    private boolean isAdd = false;

    private final Map<Integer, Marker> mMarkers = new ConcurrentHashMap<>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private boolean isPanelOpen_copy = true;
    private boolean isPanelOpen = true;
    protected ScrollView scrollView;
    protected ScrollView point_settings_scroll_view;
    protected ImageView btnPanel;

    private boolean selectedWaypoint = false;
    protected EditText change_point_v, change_point_v1;
    protected Button btn_change_commit;
    private Marker changeMarker;
    protected ImageView btn_chang_settings;
    protected TextView changePoint_text;


    //

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(mReceiver);
        removeListener();
        super.onDestroy();
    }

    /**
     * @Description : RETURN Button RESPONSE FUNCTION
     */
    public void onReturn(View view) {
        Log.d(TAG, "onReturn");
        this.finish();
    }

    private void setResultToToast(final String string) {
        Waypoint1Activity.this.runOnUiThread(() -> Toast.makeText(Waypoint1Activity.this, string, Toast.LENGTH_SHORT).show());
    }

    private void initUI() {
        editText_v = (EditText) findViewById(R.id.point_v);
        editText_v1 = (EditText) findViewById(R.id.point_v1);
        btn_commit = (Button) findViewById(R.id.btn_commit);
        btn_addPoint_mode = (Button) findViewById(R.id.btn_addPoint_mode);
        btn_clearPoint = (Button) findViewById(R.id.btn_clearPoint);
        speed_edittext = (EditText) findViewById(R.id.speed_edittxet);
        altitude_edittext = (EditText) findViewById(R.id.altitude_edittext);
        action_spinner = (Spinner) findViewById(R.id.action_spinner);
        angle_spinner = (Spinner) findViewById(R.id.angle_spinner);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_start = (Button) findViewById(R.id.btn_start);
        btn_stop = (Button) findViewById(R.id.btn_stop);

        btn_commit.setOnClickListener(this);
        btn_addPoint_mode.setOnClickListener(this);
        btn_clearPoint.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
        btn_start.setOnClickListener(this);
        btn_stop.setOnClickListener(this);

        scrollView = (ScrollView) findViewById(R.id.settings_scroll_view);
        btnPanel = (ImageView) findViewById(R.id.btn_settings);
        change_point_v = (EditText) findViewById(R.id.change_point_v);
        change_point_v1 = (EditText) findViewById(R.id.change_point_v1);
        btn_change_commit = (Button) findViewById(R.id.btn_change_commit);
        btn_chang_settings = (ImageView) findViewById(R.id.btn_change_settings);
        btn_change_commit.setOnClickListener(this);
        changePoint_text = (TextView) findViewById(R.id.changePoint_text);
        point_settings_scroll_view = (ScrollView) findViewById(R.id.point_settings_scroll_view);
    }

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }

        LatLng beijing = new LatLng(39.9149, 116.4039);
        MarkerOptions markerOption = new MarkerOptions();
        markerOption.position(beijing);
        markerOption.title("天安门");
        // 将Marker设置为贴地显示，可以双指下拉地图查看效果
        markerOption.setFlat(true);//设置marker平贴地图效果
        aMap.addMarker(markerOption);
        aMap.moveCamera(CameraUpdateFactory.newLatLng(beijing));
        // 绑定 Marker 被点击事件
        aMap.setOnMarkerClickListener(markerClickListener);
        // 绑定marker拖拽事件
        aMap.setOnMarkerDragListener(markerDragListener);
    }

    // 定义 Marker拖拽的监听
    AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {
        int index, lastIndex;

        /**
         * 当marker开始被拖动时回调此方法, 这个marker的位置可以通过getPosition()方法返回。
         * 这个位置可能与拖动的之前的marker位置不一样。
         * @param marker 被拖动的marker对象
         */
        @Override
        public void onMarkerDragStart(Marker marker) {
            index = -1;
            lastIndex = todoLineList.size();
            for (Map.Entry<Integer, Marker> mapEntry : mMarkers.entrySet()) {
                if (mapEntry.getValue().equals(marker))
                    index = mapEntry.getKey();
            }

        }

        /**
         * 在marker拖动完成后回调此方法, 这个marker的位置可以通过getPosition()方法返回
         * 这个位置可能与拖动的之前的marker位置不一样
         * @param marker 被拖动的marker对象
         */
        @Override
        public void onMarkerDragEnd(Marker marker) {
            // 更改 wayPoint
            LatLng pos = marker.getPosition();
            Waypoint waypoint = new Waypoint(pos.latitude, pos.longitude, altitude);
            waypointList.set(index, waypoint);
            // 设置lastPoint
            if (index == lastIndex)
                lastPoint = marker.getPosition();
        }

        /**
         * 在marker拖动过程中回调此方法, 这个marker的位置可以通过getPosition()方法返回
         * 这个位置可能与拖动的之前的marker位置不一样
         * @param marker 被拖动的marker对象
         */
        @Override
        public void onMarkerDrag(Marker marker) {
            if (index == -1) { // 不存在该marker
                showToast("找不到该marker");
                return;
            }
            if (index < lastIndex) { // 后面还有marker
                Polyline polyline = todoLineList.get(index);
                LatLng pos = polyline.getOptions().getPoints().get(1);
                polyline.remove();
                todoLineList.remove(index);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(marker.getPosition(), pos);
                todoLineList.add(index, aMap.addPolyline(polylineOptions));
            }
            if (index > 0) { // 前面还有marker
                Polyline polyline = todoLineList.get(index - 1);
                LatLng pos = polyline.getOptions().getPoints().get(0);
                polyline.remove();
                todoLineList.remove(index - 1);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(pos, marker.getPosition());
                todoLineList.add(index - 1, aMap.addPolyline(polylineOptions));
            }
        }
    };

    // 定义 Marker 点击事件监听
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        // marker 对象被点击时回调的接口
        // 返回 true 则表示接口已响应事件，否则返回false
        @Override
        public boolean onMarkerClick(Marker marker) {
            showToast(marker.getPosition().toString());
            selectedWaypoint = true;
            changeMarker = marker;
            showLatLng(marker);
            return true;
        }
    };

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
        initFlightController();
    }

    private PolylineOptions getFinishedPolylineOptions() {
        // 飞机已走过的线
        PolylineOptions finishedPolylineOptions = new PolylineOptions();
        finishedPolylineOptions.width(15);
        finishedPolylineOptions.color(Color.argb(255, 0, 205, 0));
        return finishedPolylineOptions;
    }

    private PolylineOptions getTodoPolylineOptions() {
        // 飞机未走过的线
        PolylineOptions todoPolylineOptions = new PolylineOptions();
        todoPolylineOptions.width(15);
        todoPolylineOptions.setDottedLine(true);
        todoPolylineOptions.setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE);
        todoPolylineOptions.color(Color.argb(255, 255, 48, 48));
        return todoPolylineOptions;
    }

    private MarkerOptions getDroneMarkerOptions() {
        // 飞机标点
        MarkerOptions droneMarkerOptions = new MarkerOptions();
        droneMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        return droneMarkerOptions;
    }

    private MarkerOptions getWayPointMarkerOptions() {
        // 航点标点
        MarkerOptions wayPointMarkerOptions = new MarkerOptions();
        wayPointMarkerOptions.draggable(true);
        wayPointMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE));
        return wayPointMarkerOptions;
    }

    protected BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            initFlightController();
        }
    };

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
                            lastDronePos = curDronePos;
                            LocationCoordinate3D location = djiFlightControllerCurrentState.getAircraftLocation();
                            curDronePos = new LatLng(location.getLatitude(), location.getLongitude());
                            updateDroneLocation();
                            if (lastDronePos != null)
                                drawPolyline(lastDronePos, curDronePos, FINISHED_LINE);
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

    private void addPointByLonLat() {
        double point_v = Double.parseDouble(editText_v.getText().toString());
        double point_v1 = Double.parseDouble(editText_v1.getText().toString());
        showToast(editText_v.toString());
        showToast(editText_v1.toString());
        if (editText_v.getText().equals("") || editText_v.getText() == null || editText_v1.getText().equals("") || editText_v1.getText() == null)
            showToast("Please Enter First");
        else {
            LatLng point = new LatLng(point_v, point_v1);
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            if (waypointMissionBuilder != null) {
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            } else {
                waypointMissionBuilder = new WaypointMission.Builder();
                waypointList.add(mWaypoint);
                waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
            }
        }
    }

    @Override
    public void onMapClick(LatLng point) {
        if (isAdd) {
            markWaypoint(point);
            Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
            //Add Waypoints to Waypoint arraylist;
            waypointList.add(mWaypoint);
            if (waypointMissionBuilder == null) {
                waypointMissionBuilder = new WaypointMission.Builder();
            }
            waypointMissionBuilder.waypointList(waypointList).waypointCount(waypointList.size());
        } else {
            setResultToToast("无法添加航点");
        }
    }


    /**
     * 检查GPS坐标
     *
     * @param pos
     * @return
     */
    public static boolean checkGpsCoordination(LatLng pos) {
        double latitude = pos.latitude, longitude = pos.longitude;
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    /**
     * 根据MCU的状态更新无人机的位置
     */
    private void updateDroneLocation() {
        if (curDronePos == null)
            return;
        MarkerOptions droneMarkerOptions = getDroneMarkerOptions();
        droneMarkerOptions.position(curDronePos);

        runOnUiThread(() -> {
            if (droneMarker != null) {
                droneMarker.remove();
            }
            if (checkGpsCoordination(curDronePos)) {
                droneMarker = aMap.addMarker(droneMarkerOptions);
            }
        });
    }

    /**
     * 绘制航线
     *
     * @param firstPoint
     * @param secondPoint
     */
    private void drawPolyline(LatLng firstPoint, LatLng secondPoint, int type) {
        if (type == TODO_LINE) {
            PolylineOptions todoPolylineOptions = getTodoPolylineOptions();
            todoPolylineOptions.add(firstPoint, secondPoint);
            Polyline polyline = aMap.addPolyline(todoPolylineOptions);
            todoLineList.add(polyline);
        } else {
            PolylineOptions finishedPolylineOptions = getFinishedPolylineOptions();
            finishedPolylineOptions.add(firstPoint, secondPoint);
            Polyline polyline = aMap.addPolyline(finishedPolylineOptions);
            finishedLineList.add(polyline);
        }
    }


    /**
     * 标记航点
     *
     * @param point
     */
    private void markWaypoint(LatLng point) {
        //Create MarkerOptions object
        MarkerOptions wayPointMarkerOptions = getWayPointMarkerOptions();
        wayPointMarkerOptions.position(point);
        // set icon
        View view = LayoutInflater.from(this).inflate(R.layout.my_icon, null);
        // set text
        ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(waypointList.size() + 1));
        wayPointMarkerOptions.icon(BitmapDescriptorFactory.fromView(view));

        Marker marker = aMap.addMarker(wayPointMarkerOptions);
        // Animation animation = new RotateAnimation(marker.getRotateAngle(),marker.getRotateAngle()+180,0,0,0);
        // long duration = 1000L;
        // animation.setDuration(duration);
        // animation.setInterpolator(new LinearInterpolator());
        // marker.setAnimation(animation);
        // marker.startAnimation();
        mMarkers.put(mMarkers.size(), marker);
        if (lastPoint != null) {
            drawPolyline(lastPoint, point, TODO_LINE);
        }
        lastPoint = point;
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_addPoint_mode) {
            enableDisableAdd();
        } else if (id == R.id.btn_clearPoint) {
            runOnUiThread(() -> aMap.clear());
            mMarkers.clear();
            waypointList.clear();
            // remove lines
            for (Polyline line : todoLineList) {
                line.remove();
            }
            todoLineList.clear();
            waypointMissionBuilder.waypointList(waypointList);
            updateDroneLocation();
            lastPoint = null;

            selectedWaypoint = false;
            changeMarker = null;
            showLatLng(changeMarker);
        } else if (id == R.id.btn_upload) {
            set_settings();
            if (!checkConditions()) {
                showToast("请检查输入的参数");
            } else {
                configWayPointMission();
                uploadWayPointMission();
            }
        } else if (id == R.id.btn_start) {
            startWaypointMission();
        } else if (id == R.id.btn_stop) {
            stopWaypointMission();
        } else if (id == R.id.btn_commit) {
            addPointByLonLat();
        } else if (id == R.id.btn_change_commit) {
            changeCommit(changeMarker);
        } else if (id == R.id.btn_settings) {
            movePanel();
        }else if (id == R.id.btn_change_settings) {
            movePanel_copy();
        }
    }

    private void cameraUpdate() {
        float zoomlevel = (float) 18.0;
        CameraUpdate cu = CameraUpdateFactory.newLatLngZoom(curDronePos, zoomlevel);
        aMap.moveCamera(cu);
    }

    private void enableDisableAdd() {
        isAdd = !isAdd;
        btn_addPoint_mode.setText(isAdd ? "Exit" : "Add");
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

    String nulltoIntegerDefalt(String value) {
        if (!isIntValue(value)) value = "0";
        return value;
    }

    boolean isIntValue(String val) {
        try {
            val = val.replace(" ", "");
            Integer.parseInt(val);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void configWayPointMission() {

        if (waypointMissionBuilder == null) {

            waypointMissionBuilder = new WaypointMission.Builder().finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        } else {
            waypointMissionBuilder.finishedAction(mFinishedAction)
                    .headingMode(mHeadingMode)
                    .autoFlightSpeed(mSpeed)
                    .maxFlightSpeed(mSpeed)
                    .flightPathMode(WaypointMissionFlightPathMode.NORMAL);

        }

        if (waypointMissionBuilder.getWaypointList().size() > 0) {

            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
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

    private void uploadWayPointMission() {

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

    private void startWaypointMission() {
        for (Polyline line : finishedLineList) {
            line.remove();
        }
        finishedLineList.clear();
        lastDronePos = null;

        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Start: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void stopWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                setResultToToast("Mission Stop: " + (error == null ? "Successfully" : error.getDescription()));
            }
        });

    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }

    private void set_settings() {
        this.mSpeed = Float.parseFloat(speed_edittext.getText().toString());
        this.altitude = Float.parseFloat(altitude_edittext.getText().toString());
        String action, angle;
        action = action_spinner.getSelectedItem().toString();
        angle = angle_spinner.getSelectedItem().toString();
        switch (action) {
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
        switch (angle) {
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

    private boolean checkConditions() {
        boolean flag = true;
        if ((mSpeed < -15) || (mSpeed > 15)) {
            showToast("飞行速度要介于-15到15之间，请重新输入");
            flag = false;
        }
        if (altitude > 120) {
            showToast("飞行高度不可过高");
            flag = false;
        }
        return flag;
    }

    private void movePanel() {
        int translationStart;
        int translationEnd;
        if (isPanelOpen) {
            translationStart = 0;
            translationEnd = -scrollView.getWidth();
        } else {

            scrollView.bringToFront();
            btnPanel.bringToFront();
            btn_chang_settings.bringToFront();
            translationStart = -scrollView.getWidth();
            translationEnd = 0;
        }
        TranslateAnimation animate = new TranslateAnimation(
                translationStart, translationEnd, 0, 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        animate.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                if (isPanelOpen) {
                    mapView.bringToFront();
                    point_settings_scroll_view.bringToFront();
                    btnPanel.bringToFront();
                    btn_chang_settings.bringToFront();
                }
                btnPanel.bringToFront();
                isPanelOpen = !isPanelOpen;

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // do nothing
            }
        });
        scrollView.startAnimation(animate);
    }

    private void movePanel_copy() {
        int translationStart;
        int translationEnd;
        if (isPanelOpen_copy) {
            translationStart = 0;
            translationEnd = +point_settings_scroll_view.getWidth();
        } else {

            point_settings_scroll_view.bringToFront();
            btnPanel.bringToFront();
            btn_chang_settings.bringToFront();
            translationStart = +point_settings_scroll_view.getWidth();
            translationEnd = 0;
        }
        TranslateAnimation animate = new TranslateAnimation(
                translationStart, translationEnd, 0, 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        animate.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
            @Override
            public void onAnimationStart(android.view.animation.Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(android.view.animation.Animation animation) {
                if (isPanelOpen_copy) {
                    mapView.bringToFront();
                    scrollView.bringToFront();
                    btnPanel.bringToFront();
                    btn_chang_settings.bringToFront();
                }
                btnPanel.bringToFront();
                isPanelOpen_copy = !isPanelOpen_copy;

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // do nothing
            }
        });
        point_settings_scroll_view.startAnimation(animate);
    }

    private void showLatLng(Marker mark) {
        if (selectedWaypoint) {
            LatLng position = mark.getPosition();
            int index;
            index = -1;
            for (Map.Entry<Integer, Marker> mapEntry : mMarkers.entrySet()) {
                if (mapEntry.getValue().equals(mark))
                    index = mapEntry.getKey();
            }
            changePoint_text.setText("更改某点坐标(点" + String.valueOf(index + 1) + ")");
            change_point_v.setText(String.valueOf(position.latitude));
            change_point_v1.setText(String.valueOf(position.longitude));
        } else {
            changePoint_text.setText("更改某点坐标");
            change_point_v.setText("请先选点");
            change_point_v1.setText("请先选点");
        }
    }

    private void changeCommit(Marker marker) {
        if (!selectedWaypoint) {
            showToast("请先选中一个点");
        } else {
            double change_v = Double.parseDouble(change_point_v.getText().toString());
            double change_v1 = Double.parseDouble(change_point_v1.getText().toString());
            LatLng position = new LatLng(change_v, change_v1);
            marker.setPosition(position);

            int index, lastIndex;
            index = -1;
            lastIndex = todoLineList.size();
            for (Map.Entry<Integer, Marker> mapEntry : mMarkers.entrySet()) {
                if (mapEntry.getValue().equals(marker))
                    index = mapEntry.getKey();
            }

            if (index == -1) { // 不存在该marker
                showToast("找不到该marker");
                return;
            }
            if (index < lastIndex) { // 后面还有marker
                Polyline polyline = todoLineList.get(index);
                LatLng pos = polyline.getOptions().getPoints().get(1);
                polyline.remove();
                todoLineList.remove(index);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(marker.getPosition(), pos);
                todoLineList.add(index, aMap.addPolyline(polylineOptions));
            }
            if (index > 0) { // 前面还有marker
                Polyline polyline = todoLineList.get(index - 1);
                LatLng pos = polyline.getOptions().getPoints().get(0);
                polyline.remove();
                todoLineList.remove(index - 1);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(pos, marker.getPosition());
                todoLineList.add(index - 1, aMap.addPolyline(polylineOptions));
            }

            LatLng pos = marker.getPosition();
            Waypoint waypoint = new Waypoint(pos.latitude, pos.longitude, altitude);
            waypointList.set(index, waypoint);
            // 设置lastPoint
            if (index == lastIndex)
                lastPoint = marker.getPosition();
        }
    }
}
