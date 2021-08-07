package com.dji.myFlight;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

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
import dji.ux.beta.core.util.SettingDefinitions;
import dji.ux.beta.core.widget.fpv.FPVWidget;

public class WaypointMissionV2Activity extends FragmentActivity implements View.OnClickListener, OnMapClickListener {

    private static final int TODO_LINE = 0;
    private static final int FINISHED_LINE = 1;

    private final float cornerRadius = 10.0f;

    private MapView mapView;
    private AMap aMap;

    private EditText etAddPointLatitude, etAddPointLongitude;
    private Button btnAddPointByLatLng, btnRemovePoint, btnAddPointByMapMode, btnClearAllPoints;
    private EditText etSpeed, etAltitude;
    private Spinner actionSpinner, angleSpinner;
    private Button btnUploadWaypointMission, btnStartWaypointMission, btnStopWaypointMission;

    // 上一个航点坐标点
    private LatLng lastPointPos = null;
    // 上一个飞机位置
    private LatLng lastDronePos = null;
    // 当前飞机位置
    private LatLng curDronePos = null;

    // 飞机已飞行轨迹列表
    private List<Polyline> finishedLineList = new ArrayList<>();
    // 航点轨迹列表
    private List<Polyline> todoLineList = new ArrayList<>();
    private Polyline firstLine;

    private boolean isAddPointMode = false;
    private boolean isMissionStarted = false;

    // 地图标记列表
    private final List<Marker> markerList = new ArrayList<>();
    private final List<Marker> auxMarkerList = new ArrayList<>();
    private Marker droneMarker = null;

    private float altitude = 100.0f;
    private float mSpeed = 10.0f;

    private List<Waypoint> waypointList = new ArrayList<>();

    public static WaypointMission.Builder waypointMissionBuilder;
    private FlightController mFlightController;
    private WaypointMissionOperator instance;
    private WaypointMissionFinishedAction mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
    private WaypointMissionHeadingMode mHeadingMode = WaypointMissionHeadingMode.AUTO;

    private boolean detailPanelVisible = true;
    private boolean isPanelOpen = true;
    protected ScrollView scrollView;
    protected ScrollView point_settings_scroll_view;
    protected ImageView btnPanel;

    protected EditText etSelectedPointLatitude, etSelectedPointLongitude;
    protected Button btnChangePointPos;
    private Marker selectedMarker;
    protected TextView tvPointDetail;
    protected ImageView btnChangeMapType;


    private UiSettings mUiSettings;//定义一个UiSettings对象

    protected FPVWidget fpvWidget;

    protected TextView tvAngleDescription;

    @Override
    protected void onResume() {
        super.onResume();
        initFlightController();
        initUI();
        fpvWidget.setVideoSource(SettingDefinitions.VideoSource.AUTO);
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
        this.finish();
    }

    private void setResultToToast(final String string) {
        WaypointMissionV2Activity.this.runOnUiThread(() -> Toast.makeText(WaypointMissionV2Activity.this, string, Toast.LENGTH_SHORT).show());
    }

    private void initUI() {
        etAddPointLatitude = (EditText) findViewById(R.id.et_add_point_latitude);
        etAddPointLongitude = (EditText) findViewById(R.id.et_add_point_longitude);
        etSelectedPointLatitude = (EditText) findViewById(R.id.selected_point_latitude);
        etSelectedPointLongitude = (EditText) findViewById(R.id.selected_point_longitude);
        etSpeed = (EditText) findViewById(R.id.et_speed);
        etAltitude = (EditText) findViewById(R.id.et_altitude);
        btnAddPointByLatLng = (Button) findViewById(R.id.btn_add_point_by_latlng);
        btnAddPointByMapMode = (Button) findViewById(R.id.btn_add_point_by_map_mode);
        btnUploadWaypointMission = (Button) findViewById(R.id.btn_upload_waypoint_mission);
        btnStartWaypointMission = (Button) findViewById(R.id.btn_start_waypoint_mission);
        btnStopWaypointMission = (Button) findViewById(R.id.btn_stop_waypoint_mission);
        btnClearAllPoints = (Button) findViewById(R.id.btn_clear_all_points);
        btnChangePointPos = (Button) findViewById(R.id.btn_change_commit);
        actionSpinner = (Spinner) findViewById(R.id.action_spinner);
        angleSpinner = (Spinner) findViewById(R.id.angle_spinner);
        scrollView = (ScrollView) findViewById(R.id.settings_scroll_view);
        btnPanel = (ImageView) findViewById(R.id.btn_settings);
        tvPointDetail = (TextView) findViewById(R.id.title_point_detail);
        point_settings_scroll_view = (ScrollView) findViewById(R.id.point_settings_scroll_view);
        btnChangeMapType = (ImageView) findViewById(R.id.btn_change_map_type);
        btnRemovePoint = (Button) findViewById(R.id.btn_remove_point);
        fpvWidget = (FPVWidget) findViewById(R.id.widget_fpv);
        tvAngleDescription = (TextView) findViewById(R.id.angle_description);

        String[] mItems1 = getResources().getStringArray(R.array.actionArray);
        String[] mItems2 = getResources().getStringArray(R.array.angleArray);
        ArrayAdapter adapter1 = new ArrayAdapter<String>(WaypointMissionV2Activity.this,
                R.layout.spinner_text, mItems1);
        ArrayAdapter adapter2 = new ArrayAdapter<String>(WaypointMissionV2Activity.this,
                R.layout.spinner_text, mItems2);
        actionSpinner.setAdapter(adapter1);
        angleSpinner.setAdapter(adapter2);

        btnStartWaypointMission.setEnabled(false);
        btnStopWaypointMission.setEnabled(false);

        btnAddPointByLatLng.setOnClickListener(this);
        btnAddPointByMapMode.setOnClickListener(this);
        btnClearAllPoints.setOnClickListener(this);
        btnUploadWaypointMission.setOnClickListener(this);
        btnStartWaypointMission.setOnClickListener(this);
        btnStopWaypointMission.setOnClickListener(this);
        btnPanel.setOnClickListener(this);
        btnChangePointPos.setOnClickListener(this);
        btnChangeMapType.setOnClickListener(this);
        btnRemovePoint.setOnClickListener(this);
        fpvWidget.setOnClickListener(this);
    }


    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            aMap.setOnMapClickListener(this); // add the listener for click for amap object
        }
        mUiSettings = aMap.getUiSettings(); //实例化UiSettings类对象
        mUiSettings.setZoomControlsEnabled(false);

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
            index = markerList.indexOf(marker);
            lastIndex = markerList.size() - 1;
        }

        /**
         * 在marker拖动完成后回调此方法, 这个marker的位置可以通过getPosition()方法返回
         * 这个位置可能与拖动的之前的marker位置不一样
         * @param marker 被拖动的marker对象
         */
        @Override
        public void onMarkerDragEnd(Marker marker) {
            // 更改 wayPoint
            changeWaypoints(index, marker.getPosition());
            // 设置lastPoint
            if (index == lastIndex)
                lastPointPos = marker.getPosition();
        }

        /**
         * 在marker拖动过程中回调此方法, 这个marker的位置可以通过getPosition()方法返回
         * 这个位置可能与拖动的之前的marker位置不一样
         * @param marker 被拖动的marker对象
         */
        @Override
        public void onMarkerDrag(Marker marker) {
            if (index == -1) { // 不存在该marker
                return;
            }
            if (index == 0) {
                firstLine.remove();
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(curDronePos, marker.getPosition());
                firstLine = aMap.addPolyline(polylineOptions);
            }
            if (index < lastIndex) { // 后面还有marker
                Polyline polyline = todoLineList.get(index);
                LatLng pos = polyline.getOptions().getPoints().get(1); // 后一坐标
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
            int index = markerList.indexOf(marker);
            if (index != -1) {
                // 更改当前点样式
                View view = LayoutInflater.from(WaypointMissionV2Activity.this).inflate(R.layout.icon_marker_selected, null);
                ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
                marker.setIcon(BitmapDescriptorFactory.fromView(view));
                // 更改前一标点样式
                if (selectedMarker != null && !marker.equals(selectedMarker)) {
                    index = markerList.indexOf(selectedMarker);
                    view = LayoutInflater.from(WaypointMissionV2Activity.this).inflate(R.layout.icon_marker, null);
                    ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
                    selectedMarker.setIcon(BitmapDescriptorFactory.fromView(view));
                }
                selectedMarker = marker;
                showSelectedMarkerDetailPanel();
            }
            return true;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_way_point_mission);

        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoApplication.FLAG_CONNECTION_CHANGE);
        registerReceiver(mReceiver, filter);

        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState);

        initMapView();
        initUI();
        addListener();
        initFlightController();

        angleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String select = parent.getItemAtPosition(position).toString();
                switch (select) {
                    case "headingNext": {
                        tvAngleDescription.setText("飞机的航向将总是沿飞行方向");
                        break;
                    }
                    case "headingInitDirec": {
                        tvAngleDescription.setText("飞机的航向将被设定为到达第一个航点时的航向。在到达第一个航点之前，飞机的航向可以由遥控器控制。当飞机到达第一个航点时，其航向将被固定");
                        break;
                    }
                    case "headingRC": {
                        tvAngleDescription.setText("飞机的航向将由远程控制器控制");
                        break;
                    }
                    case "headingWP": {
                        tvAngleDescription.setText("到达航点后，飞机将旋转其航向");
                        break;
                    }
                    default:
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        moveDetailPanel(0);
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

        BaseProduct product = DemoApplication.getProductInstance();
        if (product != null && product.isConnected()) {
            if (product instanceof Aircraft) {
                mFlightController = ((Aircraft) product).getFlightController();
            }
        }

        if (mFlightController != null) {
            mFlightController.setStateCallback(new FlightControllerState.Callback() {
                @Override
                public void onUpdate(FlightControllerState djiFlightControllerCurrentState) {
                    lastDronePos = curDronePos;
                    LocationCoordinate3D location = djiFlightControllerCurrentState.getAircraftLocation();
                    curDronePos = new LatLng(location.getLatitude(), location.getLongitude());
                    updateDroneLocation();
                    if (lastDronePos != null)
                        drawPolyline(lastDronePos, curDronePos, FINISHED_LINE);
                    if (!isMissionStarted && firstLine != null && !firstLine.getOptions().getPoints().get(0).equals(curDronePos)) {
                        firstLine.remove();
                        PolylineOptions polylineOptions = getTodoPolylineOptions();
                        polylineOptions.add(curDronePos, markerList.get(0).getPosition());
                        firstLine = aMap.addPolyline(polylineOptions);
                    }
                }
            });
            cameraUpdate();

        }
    }

    // Add Listener for WaypointMissionOperator
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
            if (error == null) {
                isMissionStarted = false;
                setResultToToast("任务完成");
            } else {
                setResultToToast("任务执行失败: " + error.getDescription());
            }
        }
    };

    public WaypointMissionOperator getWaypointMissionOperator() {
        if (instance == null) {
            instance = DJISDKManager.getInstance().getMissionControl().getWaypointMissionOperator();
        }
        return instance;
    }

    private void addPointByLonLat() {
        if (etAddPointLatitude.getText().toString().equals("") || etAddPointLongitude.getText().toString().equals("") || etAddPointLatitude.getText() == null || etAddPointLongitude.getText().equals("") || etAddPointLongitude.getText() == null) {
            showToast("请先输入经纬度");
            return;
        }
        double latitude = Double.parseDouble(etAddPointLatitude.getText().toString());
        double longitude = Double.parseDouble(etAddPointLongitude.getText().toString());
        LatLng point = new LatLng(latitude, longitude);
        markWaypoint(point);
        addWaypoints(point);
    }


    @Override
    public void onMapClick(LatLng point) {
        if (isAddPointMode) {
            markWaypoint(point);
            addWaypoints(point);
        } else if (selectedMarker != null) {
            // 更改标点样式
            int index = markerList.indexOf(selectedMarker);
            View view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
            ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
            selectedMarker.setIcon(BitmapDescriptorFactory.fromView(view));
            // 取消选中点
            selectedMarker = null;
            // 关闭标点信息面板
            if (detailPanelVisible) {
                moveDetailPanel(300);
            }
        }
    }

    /**
     * 更改地图类型
     */
    private void changeMapType() {
        switch (aMap.getMapType()) {
            case AMap.MAP_TYPE_NORMAL: {
                aMap.setMapType(AMap.MAP_TYPE_SATELLITE);
                break;
            }
            case AMap.MAP_TYPE_SATELLITE: {
                aMap.setMapType(AMap.MAP_TYPE_NIGHT);
                break;
            }
            case AMap.MAP_TYPE_NIGHT: {
                aMap.setMapType(AMap.MAP_TYPE_NORMAL);
                break;
            }
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
        View view;
        // 更改前一标点样式
        if (selectedMarker != null && !markerList.isEmpty()) {
            int index = markerList.indexOf(selectedMarker);
            view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
            ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
            selectedMarker.setIcon(BitmapDescriptorFactory.fromView(view));
        }
        // set icon
        view = LayoutInflater.from(this).inflate(R.layout.icon_marker_selected, null);
        // set text
        ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(markerList.size() + 1));
        wayPointMarkerOptions.icon(BitmapDescriptorFactory.fromView(view));

        Marker marker = aMap.addMarker(wayPointMarkerOptions);
        // Animation animation = new RotateAnimation(marker.getRotateAngle(),marker.getRotateAngle()+180,0,0,0);
        // long duration = 1000L;
        // animation.setDuration(duration);
        // animation.setInterpolator(new LinearInterpolator());
        // marker.setAnimation(animation);
        // marker.startAnimation();
        markerList.add(marker);
        if (lastPointPos != null) {
            drawPolyline(lastPointPos, point, TODO_LINE);
        } else if (curDronePos != null) {
            PolylineOptions todoPolylineOptions = getTodoPolylineOptions();
            todoPolylineOptions.add(curDronePos, point);
            firstLine = aMap.addPolyline(todoPolylineOptions);
        }
        lastPointPos = point;
        // 显示信息面板
        selectedMarker = marker;
        showSelectedMarkerDetailPanel();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_add_point_by_map_mode) {
            enableDisableAdd();
        } else if (id == R.id.btn_clear_all_points) {
            runOnUiThread(() -> aMap.clear());
            markerList.clear();
            auxMarkerList.clear();
            // 移除画线
            for (Polyline line : todoLineList) {
                line.remove();
            }
            todoLineList.clear();
            for (Polyline line : finishedLineList) {
                line.remove();
            }
            finishedLineList.clear();
            if (firstLine != null) {
                firstLine.remove();
                firstLine = null;
            }
            // 清空航点
            waypointList.clear();
            updateDroneLocation();
            lastPointPos = null;
            // 关闭标点信息面板
            if (detailPanelVisible) {
                moveDetailPanel(300);
            }
            btnUploadWaypointMission.setEnabled(true);
            btnStartWaypointMission.setEnabled(false);
            btnStopWaypointMission.setEnabled(false);
        } else if (id == R.id.btn_upload_waypoint_mission) {
            set_settings();
            if (checkConditions()) {
                configAndUploadWayPointMission();
            }
        } else if (id == R.id.btn_start_waypoint_mission) {
            startWaypointMission();
        } else if (id == R.id.btn_stop_waypoint_mission) {
            stopWaypointMission();
        } else if (id == R.id.btn_add_point_by_latlng) {
            addPointByLonLat();
        } else if (id == R.id.btn_change_commit) {
            changePointPos();
        } else if (id == R.id.btn_settings) {
            movePanel();
        } else if (id == R.id.btn_change_map_type) {
            changeMapType();
        } else if (id == R.id.btn_remove_point) {
            removePoint();
        } else if (id == R.id.widget_fpv) {
            Intent intent = new Intent(this, FPVForWayPointActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        }
    }

    //改变可视区域为指定位置
    private void cameraUpdate() {

        float zoomLevel = (float) 18.0;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(curDronePos, zoomLevel);
        aMap.moveCamera(cameraUpdate);
    }

    private void enableDisableAdd() {
        isAddPointMode = !isAddPointMode;
        btnAddPointByMapMode.setText(isAddPointMode ? "关闭地图选点" : "地图选点");
    }

    private void configAndUploadWayPointMission() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder();
        }
        waypointMissionBuilder.finishedAction(mFinishedAction)
                .headingMode(mHeadingMode)
                .autoFlightSpeed(mSpeed)
                .maxFlightSpeed(mSpeed)
                .flightPathMode(WaypointMissionFlightPathMode.CURVED)
                .waypointList(waypointList)
                .waypointCount(waypointList.size());
        // waypointMissionBuilder.addWaypoint(waypointList.get(0));

        if (waypointMissionBuilder.getWaypointList().size() > 0) {
            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }
            setResultToToast("成功设置航点高度");
        }
        btnStartWaypointMission.setEnabled(false);
        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("成功加载航点");
            // 上传任务
            getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        setResultToToast("任务上传成功");
                        setButtonAbility(btnStartWaypointMission, true);
                    } else {
                        setResultToToast("任务上传失败: " + error.getDescription() + ", 重试中...");
                        getWaypointMissionOperator().retryUploadMission(null);
                    }
                }
            });
        } else {
            setResultToToast("加载航点失败: " + error.getDescription());
        }
        btnStartWaypointMission.setEnabled(true);
    }

    private void startWaypointMission() {
        for (int i = 0; i < finishedLineList.size(); i++) {
            finishedLineList.remove(i);
        }
        finishedLineList.clear();
        lastDronePos = null;
        getWaypointMissionOperator().startMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    isMissionStarted = true;
                    setResultToToast("任务已启动");
                    setButtonAbility(btnStartWaypointMission, false);
                    setButtonAbility(btnUploadWaypointMission, false);
                    setButtonAbility(btnStopWaypointMission, true);
                } else {
                    setResultToToast("启动任务失败: " + error.getDescription());
                }
            }
        });
    }

    private void setButtonAbility(Button btn, boolean b) {
        WaypointMissionV2Activity.this.runOnUiThread(() -> btn.setEnabled(b));
    }

    private void stopWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    isMissionStarted = false;
                    setResultToToast("已停止任务");
                    setButtonAbility(btnStopWaypointMission, false);
                    setButtonAbility(btnUploadWaypointMission, true);
                } else {
                    setResultToToast("停止任务失败 " + error.getDescription());
                }
            }
        });

    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }

    private void set_settings() {

        if ((etSpeed.getText().toString().equals("")) || (etAltitude.getText().toString().equals(""))) {
            showToast("您还没有输入速度或者高度的值");
        } else {
            this.mSpeed = Float.parseFloat(etSpeed.getText().toString());
            this.altitude = Float.parseFloat(etAltitude.getText().toString());
        }
        String action, angle;
        action = actionSpinner.getSelectedItem().toString();
        angle = angleSpinner.getSelectedItem().toString();
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
        if (waypointList.size() < 2) {
            showToast("航点不能少于2个");
            return false;
        }
        if ((mSpeed < -15) || (mSpeed > 15)) {
            showToast("飞行速度要介于-15到15之间，请重新输入");
            return false;
        }
        if (altitude > 120) {
            showToast("飞行高度不可过高");
            return false;
        }
        return true;
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
            btnChangeMapType.bringToFront();
            fpvWidget.bringToFront();
            translationStart = -scrollView.getWidth();
            translationEnd = 0;
        }
        TranslateAnimation animate = new TranslateAnimation(
                translationStart, translationEnd, 0, 0);
        animate.setDuration(300);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (isPanelOpen) {
                    mapView.bringToFront();
                    if (selectedMarker != null) {
                        point_settings_scroll_view.bringToFront();
                    }
                    btnPanel.bringToFront();
                    btnChangeMapType.bringToFront();
                    fpvWidget.bringToFront();
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

    private void moveDetailPanel(long time) {
        int translationStart;
        int translationEnd;
        if (detailPanelVisible) {
            translationStart = 0;
            translationEnd = +point_settings_scroll_view.getWidth();
        } else {

            point_settings_scroll_view.bringToFront();
            btnPanel.bringToFront();
            btnChangeMapType.bringToFront();
            fpvWidget.bringToFront();
            translationStart = +point_settings_scroll_view.getWidth();
            translationEnd = 0;
        }
        TranslateAnimation animate = new TranslateAnimation(
                translationStart, translationEnd, 0, 0);
        animate.setDuration(time);
        animate.setFillAfter(true);
        animate.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                // do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (detailPanelVisible) {
                    mapView.bringToFront();
                    scrollView.bringToFront();
                    btnPanel.bringToFront();
                    btnChangeMapType.bringToFront();
                    fpvWidget.bringToFront();
                }
                btnPanel.bringToFront();
                detailPanelVisible = !detailPanelVisible;

            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                // do nothing
            }
        });
        point_settings_scroll_view.startAnimation(animate);
    }

    private void showSelectedMarkerDetailPanel() {
        LatLng position = selectedMarker.getPosition();
        int index = markerList.indexOf(selectedMarker);
        tvPointDetail.setText(String.format("航点%d", index + 1));
        etSelectedPointLatitude.setText(String.valueOf(position.latitude));
        etSelectedPointLongitude.setText(String.valueOf(position.longitude));
        if (!detailPanelVisible) {
            moveDetailPanel(300);
        }
    }

    private void changePointPos() {
        if (selectedMarker == null) {
            showToast("请先选中一个点");
            return;
        }
        if ((etSelectedPointLatitude.getText().toString().equals("")) || (etSelectedPointLongitude.getText().toString().equals(""))) {
            showToast("请先输入经纬度");
        } else {
            double latitude = Double.parseDouble(etSelectedPointLatitude.getText().toString());
            double longitude = Double.parseDouble(etSelectedPointLongitude.getText().toString());
            LatLng position = new LatLng(latitude, longitude);
            selectedMarker.setPosition(position);

            int index = markerList.indexOf(selectedMarker), lastIndex = markerList.size() - 1;
            if (index == -1) { // 不存在该marker
                showToast("找不到该marker");
                return;
            }
            if (index == 0) {
                firstLine.remove();
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(curDronePos, markerList.get(0).getPosition());
                firstLine = aMap.addPolyline(polylineOptions);
            }
            if (index < lastIndex) { // 后面还有marker
                Polyline polyline = todoLineList.get(index);
                LatLng pos = polyline.getOptions().getPoints().get(1);
                polyline.remove();
                todoLineList.remove(index);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(selectedMarker.getPosition(), pos);
                todoLineList.add(index, aMap.addPolyline(polylineOptions));
            }
            if (index > 0) { // 前面还有marker
                Polyline polyline = todoLineList.get(index - 1);
                LatLng pos = polyline.getOptions().getPoints().get(0);
                polyline.remove();
                todoLineList.remove(index - 1);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(pos, selectedMarker.getPosition());
                todoLineList.add(index - 1, aMap.addPolyline(polylineOptions));
            }
            changeWaypoints(index, position);
            // 设置lastPoint
            if (index == lastIndex)
                lastPointPos = selectedMarker.getPosition();
        }
    }

    /**
     * 清除某一标记
     */
    private void removePoint() {
        View view;
        if (selectedMarker == null || !markerList.contains(selectedMarker)) { // 不存在该标记
            showToast("找不到航点");
        } else {
            int index = markerList.indexOf(selectedMarker), lastIndex = markerList.size() - 1;
            // 移除标记
            selectedMarker.remove();
            markerList.remove(index);
            if (index == 0 && lastIndex == 0) { // 只有一个标记
                firstLine.remove();
                firstLine = null;
                // 设置前一个坐标点
                lastPointPos = null;
            } else if (index == 0) { // 第一个标记
                // 移除画线
                Polyline polyline = todoLineList.get(0);
                polyline.remove();
                todoLineList.remove(index);
                if (curDronePos != null) {
                    firstLine.remove();
                    firstLine = null;
                }
                if (lastIndex > 0) {
                    // 重新画线
                    PolylineOptions polylineOptions = getTodoPolylineOptions();
                    polylineOptions.add(curDronePos, markerList.get(0).getPosition());
                    firstLine = aMap.addPolyline(polylineOptions);
                }
                // 更新后续标记数字
                for (int i = index; i < lastIndex; i++) {
                    view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
                    ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(i + 1));
                    markerList.get(i).setIcon(BitmapDescriptorFactory.fromView(view));
                }
            } else if (index == lastIndex) { // 最后一个标记
                // 移除画线
                Polyline polyline = todoLineList.get(index - 1);
                polyline.remove();
                todoLineList.remove(index - 1);
                // 设置前一个坐标点
                lastPointPos = markerList.get(lastIndex - 1).getPosition();
            } else { // 中间标记
                // 移除画线 并重新画线连接前后两标记
                Polyline prevLine = todoLineList.get(index - 1);
                Polyline nextLine = todoLineList.get(index);
                LatLng prevPos = prevLine.getOptions().getPoints().get(0);
                LatLng nextPos = nextLine.getOptions().getPoints().get(1);
                prevLine.remove();
                nextLine.remove();
                todoLineList.remove(index - 1);
                todoLineList.remove(index - 1);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(prevPos, nextPos);
                todoLineList.add(index - 1, aMap.addPolyline(polylineOptions));
                // 更新后续标记数字
                for (int i = index; i < lastIndex; i++) {
                    view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
                    ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(i + 1));
                    markerList.get(i).setIcon(BitmapDescriptorFactory.fromView(view));
                }
            }
            // 选中点设置为null
            selectedMarker = null;
            // 移除航点
            for (int i = 0; i < 6; i++) {
                waypointList.remove(index * 6);
                auxMarkerList.get(index * 6).remove();
                auxMarkerList.remove(index * 6);
            }
            // 关闭标点信息面板
            if (detailPanelVisible) {
                moveDetailPanel(300);
            }
        }
    }

    public void addWaypoints(LatLng point) {
        double biasLat = 0.00001 / 1.3 * cornerRadius;
        double biasLng = 0.00001 * cornerRadius;
        Waypoint waypoint1 = new Waypoint(point.latitude, point.longitude - biasLng, altitude);
        Waypoint waypoint2 = new Waypoint(point.latitude - biasLat, point.longitude - biasLng, altitude);
        waypoint2.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint3 = new Waypoint(point.latitude - biasLat, point.longitude + biasLng, altitude);
        waypoint3.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint4 = new Waypoint(point.latitude + biasLat, point.longitude + biasLng, altitude);
        waypoint4.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint5 = new Waypoint(point.latitude + biasLat, point.longitude - biasLng, altitude);
        waypoint5.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint6 = new Waypoint(point.latitude, point.longitude - biasLng, altitude);
        waypointList.add(waypoint1);
        waypointList.add(waypoint2);
        waypointList.add(waypoint3);
        waypointList.add(waypoint4);
        waypointList.add(waypoint5);
        waypointList.add(waypoint6);
        MarkerOptions markerOptions = getWayPointMarkerOptions();
        markerOptions.position(new LatLng(point.latitude, point.longitude - biasLng)).draggable(false);
        auxMarkerList.add(aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude - biasLat, point.longitude - biasLng)).draggable(false);
        auxMarkerList.add(aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude - biasLat, point.longitude + biasLng)).draggable(false);
        auxMarkerList.add(aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude + biasLat, point.longitude + biasLng)).draggable(false);
        auxMarkerList.add(aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude + biasLat, point.longitude - biasLng)).draggable(false);
        auxMarkerList.add(aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude, point.longitude - biasLng)).draggable(false);
        auxMarkerList.add(aMap.addMarker(markerOptions));
    }

    public void changeWaypoints(int index, LatLng point) {
        double biasLat = 0.00001 / 1.3 * cornerRadius;
        double biasLng = 0.00001 * cornerRadius;
        Waypoint waypoint1 = new Waypoint(point.latitude, point.longitude - biasLng, altitude);
        Waypoint waypoint2 = new Waypoint(point.latitude - biasLat, point.longitude - biasLng, altitude);
        waypoint2.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint3 = new Waypoint(point.latitude - biasLat, point.longitude + biasLng, altitude);
        waypoint3.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint4 = new Waypoint(point.latitude + biasLat, point.longitude + biasLng, altitude);
        waypoint4.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint5 = new Waypoint(point.latitude + biasLat, point.longitude - biasLng, altitude);
        waypoint5.cornerRadiusInMeters = cornerRadius - 2;
        Waypoint waypoint6 = new Waypoint(point.latitude, point.longitude - biasLng, altitude);
        waypointList.set(index * 6, waypoint1);
        waypointList.set(index * 6 + 1, waypoint2);
        waypointList.set(index * 6 + 2, waypoint3);
        waypointList.set(index * 6 + 3, waypoint4);
        waypointList.set(index * 6 + 4, waypoint5);
        waypointList.set(index * 6 + 5, waypoint6);

        for (int i = 0; i < 6; i++) {
            auxMarkerList.get(index * 6 + i).remove();
        }
        MarkerOptions markerOptions = getWayPointMarkerOptions();
        markerOptions.draggable(false);
        markerOptions.position(new LatLng(point.latitude, point.longitude - biasLng));
        auxMarkerList.set(index * 6, aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude - biasLat, point.longitude - biasLng));
        auxMarkerList.set(index * 6 + 1, aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude - biasLat, point.longitude + biasLng));
        auxMarkerList.set(index * 6 + 2, aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude + biasLat, point.longitude + biasLng));
        auxMarkerList.set(index * 6 + 3, aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude + biasLat, point.longitude - biasLng));
        auxMarkerList.set(index * 6 + 4, aMap.addMarker(markerOptions));
        markerOptions.position(new LatLng(point.latitude, point.longitude - biasLng));
        auxMarkerList.set(index * 6 + 5, aMap.addMarker(markerOptions));
        // auxMarkerList.set(index * 6, aMap.addMarker(getWayPointMarkerOptions()
        //                 .position(new LatLng(point.latitude, point.longitude - biasLng))
        //                 .draggable(false)
        //         )
        // );
        // auxMarkerList.set(index * 6 + 1, aMap.addMarker(getWayPointMarkerOptions()
        //                 .position(new LatLng(point.latitude - biasLat, point.longitude - biasLng))
        //                 .draggable(false)
        //         )
        // );
        // auxMarkerList.set(index * 6 + 2, aMap.addMarker(getWayPointMarkerOptions()
        //                 .position(new LatLng(point.latitude - biasLat, point.longitude + biasLng))
        //                 .draggable(false)
        //         )
        // );
        // auxMarkerList.set(index * 6 + 3, aMap.addMarker(getWayPointMarkerOptions()
        //                 .position(new LatLng(point.latitude + biasLat, point.longitude + biasLng))
        //                 .draggable(false)
        //         )
        // );
        // auxMarkerList.set(index * 6 + 4, aMap.addMarker(getWayPointMarkerOptions()
        //                 .position(new LatLng(point.latitude + biasLat, point.longitude - biasLng))
        //                 .draggable(false)
        //         )
        // );
        // auxMarkerList.set(index * 6 + 5, aMap.addMarker(getWayPointMarkerOptions()
        //                 .position(new LatLng(point.latitude, point.longitude - biasLng))
        //                 .draggable(false)
        //         )
        // );
    }
}
