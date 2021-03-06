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

public class WaypointMissionActivity extends FragmentActivity implements View.OnClickListener, OnMapClickListener {

    private static final int TODO_LINE = 0;
    private static final int FINISHED_LINE = 1;

    private MapView mapView;
    private AMap aMap;

    private EditText etAddPointLatitude, etAddPointLongitude;
    private Button btnAddPointByLatLng, btnRemovePoint, btnAddPointByMapMode, btnClearAllPoints;
    private EditText etSpeed, etAltitude;
    private Spinner actionSpinner, angleSpinner;
    private Button btnUploadWaypointMission, btnStartWaypointMission, btnStopWaypointMission;

    // ????????????????????????
    private LatLng lastPointPos = null;
    // ?????????????????????
    private LatLng lastDronePos = null;
    // ??????????????????
    private LatLng curDronePos = null;

    // ???????????????????????????
    private List<Polyline> finishedLineList = new ArrayList<>();
    // ??????????????????
    private List<Polyline> todoLineList = new ArrayList<>();
    private Polyline firstLine;

    private boolean isAdd = false;
    private boolean isMissionStarted = false;

    // ??????????????????
    private final List<Marker> markerList = new ArrayList<>();
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


    private UiSettings mUiSettings;//????????????UiSettings??????

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
        WaypointMissionActivity.this.runOnUiThread(() -> Toast.makeText(WaypointMissionActivity.this, string, Toast.LENGTH_SHORT).show());
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
        ArrayAdapter adapter1 = new ArrayAdapter<String>(WaypointMissionActivity.this,
                R.layout.spinner_text, mItems1);
        ArrayAdapter adapter2 = new ArrayAdapter<String>(WaypointMissionActivity.this,
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
        mUiSettings = aMap.getUiSettings(); //?????????UiSettings?????????
        mUiSettings.setZoomControlsEnabled(false);

        // ?????? Marker ???????????????
        aMap.setOnMarkerClickListener(markerClickListener);
        // ??????marker????????????
        aMap.setOnMarkerDragListener(markerDragListener);
    }

    // ?????? Marker???????????????
    AMap.OnMarkerDragListener markerDragListener = new AMap.OnMarkerDragListener() {
        int index, lastIndex;

        /**
         * ???marker?????????????????????????????????, ??????marker?????????????????????getPosition()???????????????
         * ???????????????????????????????????????marker??????????????????
         * @param marker ????????????marker??????
         */
        @Override
        public void onMarkerDragStart(Marker marker) {
            index = markerList.indexOf(marker);
            lastIndex = markerList.size() - 1;
        }

        /**
         * ???marker??????????????????????????????, ??????marker?????????????????????getPosition()????????????
         * ???????????????????????????????????????marker???????????????
         * @param marker ????????????marker??????
         */
        @Override
        public void onMarkerDragEnd(Marker marker) {
            // ?????? wayPoint
            LatLng pos = marker.getPosition();
            Waypoint waypoint = new Waypoint(pos.latitude, pos.longitude, altitude);
            waypointList.set(index, waypoint);
            // ??????lastPoint
            if (index == lastIndex)
                lastPointPos = marker.getPosition();
        }

        /**
         * ???marker??????????????????????????????, ??????marker?????????????????????getPosition()????????????
         * ???????????????????????????????????????marker???????????????
         * @param marker ????????????marker??????
         */
        @Override
        public void onMarkerDrag(Marker marker) {
            if (index == -1) { // ????????????marker
                showToast("????????????marker");
                return;
            }
            if (index == 0) {
                firstLine.remove();
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(curDronePos, marker.getPosition());
                firstLine = aMap.addPolyline(polylineOptions);
            }
            if (index < lastIndex) { // ????????????marker
                Polyline polyline = todoLineList.get(index);
                LatLng pos = polyline.getOptions().getPoints().get(1); // ????????????
                polyline.remove();
                todoLineList.remove(index);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(marker.getPosition(), pos);
                todoLineList.add(index, aMap.addPolyline(polylineOptions));
            }
            if (index > 0) { // ????????????marker
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

    // ?????? Marker ??????????????????
    AMap.OnMarkerClickListener markerClickListener = new AMap.OnMarkerClickListener() {
        // marker ?????????????????????????????????
        // ?????? true ?????????????????????????????????????????????false
        @Override
        public boolean onMarkerClick(Marker marker) {
            int index = markerList.indexOf(marker);
            // ?????????????????????
            View view = LayoutInflater.from(WaypointMissionActivity.this).inflate(R.layout.icon_marker_selected, null);
            ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
            marker.setIcon(BitmapDescriptorFactory.fromView(view));
            // ????????????????????????
            if (selectedMarker != null && !marker.equals(selectedMarker)) {
                index = markerList.indexOf(selectedMarker);
                view = LayoutInflater.from(WaypointMissionActivity.this).inflate(R.layout.icon_marker, null);
                ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
                selectedMarker.setIcon(BitmapDescriptorFactory.fromView(view));
            }
            selectedMarker = marker;
            showSelectedMarkerDetailPanel();
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
                        tvAngleDescription.setText("???????????????????????????????????????");
                        break;
                    }
                    case "headingInitDirec": {
                        tvAngleDescription.setText("??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????");
                        break;
                    }
                    case "headingRC": {
                        tvAngleDescription.setText("??????????????????????????????????????????");
                        break;
                    }
                    case "headingWP": {
                        tvAngleDescription.setText("??????????????????????????????????????????");
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
        // ?????????????????????
        PolylineOptions finishedPolylineOptions = new PolylineOptions();
        finishedPolylineOptions.width(15);
        finishedPolylineOptions.color(Color.argb(255, 0, 205, 0));
        return finishedPolylineOptions;
    }

    private PolylineOptions getTodoPolylineOptions() {
        // ?????????????????????
        PolylineOptions todoPolylineOptions = new PolylineOptions();
        todoPolylineOptions.width(15);
        todoPolylineOptions.setDottedLine(true);
        todoPolylineOptions.setDottedLineType(PolylineOptions.DOTTEDLINE_TYPE_SQUARE);
        todoPolylineOptions.color(Color.argb(255, 255, 48, 48));
        return todoPolylineOptions;
    }

    private MarkerOptions getDroneMarkerOptions() {
        // ????????????
        MarkerOptions droneMarkerOptions = new MarkerOptions();
        droneMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.aircraft));
        return droneMarkerOptions;
    }

    private MarkerOptions getWayPointMarkerOptions() {
        // ????????????
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
                setResultToToast("????????????");
            } else {
                setResultToToast("??????????????????: " + error.getDescription());
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
        if ((etAddPointLatitude.getText().toString().equals("")) || (etAddPointLongitude.getText().toString().equals(""))) {
            showToast("?????????????????????");
        } else {
            double point_v = Double.parseDouble(etAddPointLatitude.getText().toString());
            double point_v1 = Double.parseDouble(etAddPointLongitude.getText().toString());
            if (etAddPointLatitude.getText().equals("") || etAddPointLatitude.getText() == null || etAddPointLongitude.getText().equals("") || etAddPointLongitude.getText() == null)
                showToast("Please Enter First");
            else {
                LatLng point = new LatLng(point_v, point_v1);
                markWaypoint(point);
                Waypoint mWaypoint = new Waypoint(point.latitude, point.longitude, altitude);
                waypointList.add(mWaypoint);
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
        } else if (selectedMarker != null) {
            // ??????????????????
            int index = markerList.indexOf(selectedMarker);
            View view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
            ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
            selectedMarker.setIcon(BitmapDescriptorFactory.fromView(view));
            // ???????????????
            selectedMarker = null;
            // ????????????????????????
            if (detailPanelVisible) {
                moveDetailPanel(300);
            }
        }
    }

    /**
     * ??????????????????
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
     * ??????GPS??????
     *
     * @param pos
     * @return
     */
    public static boolean checkGpsCoordination(LatLng pos) {
        double latitude = pos.latitude, longitude = pos.longitude;
        return (latitude > -90 && latitude < 90 && longitude > -180 && longitude < 180) && (latitude != 0f && longitude != 0f);
    }

    /**
     * ??????MCU?????????????????????????????????
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
     * ????????????
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
     * ????????????
     *
     * @param point
     */
    private void markWaypoint(LatLng point) {
        //Create MarkerOptions object
        MarkerOptions wayPointMarkerOptions = getWayPointMarkerOptions();
        wayPointMarkerOptions.position(point);
        View view;
        // ????????????????????????
        if (selectedMarker != null && !markerList.isEmpty()) {
            int index = markerList.indexOf(selectedMarker);
            view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
            ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(index + 1));
            selectedMarker.setIcon(BitmapDescriptorFactory.fromView(view));
        }
        // set icon
        view = LayoutInflater.from(this).inflate(R.layout.icon_marker_selected, null);
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
        markerList.add(marker);
        if (lastPointPos != null) {
            drawPolyline(lastPointPos, point, TODO_LINE);
        } else if (curDronePos != null) {
            PolylineOptions todoPolylineOptions = getTodoPolylineOptions();
            todoPolylineOptions.add(curDronePos, point);
            firstLine = aMap.addPolyline(todoPolylineOptions);
        }
        lastPointPos = point;
        // ??????????????????
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
            // ????????????
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
            // ????????????
            waypointList.clear();
            updateDroneLocation();
            lastPointPos = null;
            // ????????????????????????
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

    //?????????????????????????????????
    private void cameraUpdate() {

        float zoomLevel = (float) 18.0;
        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(curDronePos, zoomLevel);
        aMap.moveCamera(cameraUpdate);
    }

    private void enableDisableAdd() {
        isAdd = !isAdd;
        btnAddPointByMapMode.setText(isAdd ? "??????????????????" : "????????????");
    }

    private void configAndUploadWayPointMission() {
        if (waypointMissionBuilder == null) {
            waypointMissionBuilder = new WaypointMission.Builder();
        }
        waypointMissionBuilder.finishedAction(mFinishedAction)
                .headingMode(mHeadingMode)
                .autoFlightSpeed(mSpeed)
                .maxFlightSpeed(mSpeed)
                .flightPathMode(WaypointMissionFlightPathMode.NORMAL)
                .waypointList(waypointList)
                .waypointCount(waypointList.size());

        if (waypointMissionBuilder.getWaypointList().size() > 0) {
            for (int i = 0; i < waypointMissionBuilder.getWaypointList().size(); i++) {
                waypointMissionBuilder.getWaypointList().get(i).altitude = altitude;
            }
            setResultToToast("????????????????????????");
        }
        btnStartWaypointMission.setEnabled(false);
        DJIError error = getWaypointMissionOperator().loadMission(waypointMissionBuilder.build());
        if (error == null) {
            setResultToToast("??????????????????");
            // ????????????
            getWaypointMissionOperator().uploadMission(new CommonCallbacks.CompletionCallback() {
                @Override
                public void onResult(DJIError error) {
                    if (error == null) {
                        setResultToToast("??????????????????");
                        setButtonAbility(btnStartWaypointMission, true);
                    } else {
                        setResultToToast("??????????????????: " + error.getDescription() + ", ?????????...");
                        getWaypointMissionOperator().retryUploadMission(null);
                    }
                }
            });
        } else {
            setResultToToast("??????????????????: " + error.getDescription());
        }
        btnStartWaypointMission.setEnabled(true);
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
                if (error == null) {
                    isMissionStarted = true;
                    setResultToToast("???????????????");
                    setButtonAbility(btnStartWaypointMission, false);
                    setButtonAbility(btnUploadWaypointMission, false);
                    setButtonAbility(btnStopWaypointMission, true);
                } else {
                    setResultToToast("??????????????????: " + error.getDescription());
                }
            }
        });
    }

    private void setButtonAbility(Button btn, boolean b) {
        WaypointMissionActivity.this.runOnUiThread(() -> btn.setEnabled(b));
    }

    private void stopWaypointMission() {

        getWaypointMissionOperator().stopMission(new CommonCallbacks.CompletionCallback() {
            @Override
            public void onResult(DJIError error) {
                if (error == null) {
                    isMissionStarted = false;
                    setResultToToast("???????????????");
                    setButtonAbility(btnStopWaypointMission, false);
                    setButtonAbility(btnUploadWaypointMission, true);
                } else {
                    setResultToToast("?????????????????? " + error.getDescription());
                }
            }
        });

    }

    private void showToast(final String toastMsg) {
        runOnUiThread(() -> Toast.makeText(getApplicationContext(), toastMsg, Toast.LENGTH_LONG).show());
    }

    private void set_settings() {

        if ((etSpeed.getText().toString().equals("")) || (etAltitude.getText().toString().equals(""))) {
            showToast("??????????????????????????????????????????");
        } else {
            this.mSpeed = Float.parseFloat(etSpeed.getText().toString());
            this.altitude = Float.parseFloat(etAltitude.getText().toString());
        }
        String action, angle;
        action = actionSpinner.getSelectedItem().toString();
        angle = angleSpinner.getSelectedItem().toString();
        switch (action) {
            case "?????????":
                mFinishedAction = WaypointMissionFinishedAction.NO_ACTION;
                break;
            case "????????????":
                mFinishedAction = WaypointMissionFinishedAction.GO_HOME;
                break;
            case "????????????":
                mFinishedAction = WaypointMissionFinishedAction.AUTO_LAND;
                break;
            case "?????????????????????":
                mFinishedAction = WaypointMissionFinishedAction.GO_FIRST_WAYPOINT;
                break;
            default:
                showToast("??????????????????");
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
                showToast("????????????????????????");
                break;
        }
    }

    private boolean checkConditions() {
        if (waypointList.size() < 2) {
            showToast("??????????????????2???");
            return false;
        }
        if ((mSpeed < -15) || (mSpeed > 15)) {
            showToast("?????????????????????-15???15????????????????????????");
            return false;
        }
        if (altitude > 120) {
            showToast("????????????????????????");
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
        tvPointDetail.setText(String.format("??????%d", index + 1));
        etSelectedPointLatitude.setText(String.valueOf(position.latitude));
        etSelectedPointLongitude.setText(String.valueOf(position.longitude));
        if (!detailPanelVisible) {
            moveDetailPanel(300);
        }
    }

    private void changePointPos() {
        if (selectedMarker == null) {
            showToast("?????????????????????");
            return;
        }
        if ((etSelectedPointLatitude.getText().toString().equals("")) || (etSelectedPointLongitude.getText().toString().equals(""))) {
            showToast("?????????????????????");
        } else {
            double latitude = Double.parseDouble(etSelectedPointLatitude.getText().toString());
            double longitude = Double.parseDouble(etSelectedPointLongitude.getText().toString());
            LatLng position = new LatLng(latitude, longitude);
            selectedMarker.setPosition(position);

            int index = markerList.indexOf(selectedMarker), lastIndex = markerList.size() - 1;
            if (index == -1) { // ????????????marker
                showToast("????????????marker");
                return;
            }
            if (index == 0) {
                firstLine.remove();
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(curDronePos, markerList.get(0).getPosition());
                firstLine = aMap.addPolyline(polylineOptions);
            }
            if (index < lastIndex) { // ????????????marker
                Polyline polyline = todoLineList.get(index);
                LatLng pos = polyline.getOptions().getPoints().get(1);
                polyline.remove();
                todoLineList.remove(index);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(selectedMarker.getPosition(), pos);
                todoLineList.add(index, aMap.addPolyline(polylineOptions));
            }
            if (index > 0) { // ????????????marker
                Polyline polyline = todoLineList.get(index - 1);
                LatLng pos = polyline.getOptions().getPoints().get(0);
                polyline.remove();
                todoLineList.remove(index - 1);
                PolylineOptions polylineOptions = getTodoPolylineOptions();
                polylineOptions.add(pos, selectedMarker.getPosition());
                todoLineList.add(index - 1, aMap.addPolyline(polylineOptions));
            }
            Waypoint waypoint = new Waypoint(position.latitude, position.longitude, altitude);
            waypointList.set(index, waypoint);
            // ??????lastPoint
            if (index == lastIndex)
                lastPointPos = selectedMarker.getPosition();
        }
    }

    /**
     * ??????????????????
     */
    private void removePoint() {
        View view;
        if (selectedMarker == null || !markerList.contains(selectedMarker)) { // ??????????????????
            showToast("???????????????");
        } else {
            int index = markerList.indexOf(selectedMarker), lastIndex = markerList.size() - 1;
            // ????????????
            selectedMarker.remove();
            markerList.remove(index);
            if (index == 0 && lastIndex == 0) { // ??????????????????
                firstLine.remove();
                firstLine = null;
                // ????????????????????????
                lastPointPos = null;
            } else if (index == 0) { // ???????????????
                // ????????????
                Polyline polyline = todoLineList.get(0);
                polyline.remove();
                todoLineList.remove(index);
                if (curDronePos != null) {
                    firstLine.remove();
                    firstLine = null;
                }
                if (lastIndex > 0) {
                    // ????????????
                    PolylineOptions polylineOptions = getTodoPolylineOptions();
                    polylineOptions.add(curDronePos, markerList.get(0).getPosition());
                    firstLine = aMap.addPolyline(polylineOptions);
                }
                // ????????????????????????
                for (int i = index; i < lastIndex; i++) {
                    view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
                    ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(i + 1));
                    markerList.get(i).setIcon(BitmapDescriptorFactory.fromView(view));
                }
            } else if (index == lastIndex) { // ??????????????????
                // ????????????
                Polyline polyline = todoLineList.get(index - 1);
                polyline.remove();
                todoLineList.remove(index - 1);
                // ????????????????????????
                lastPointPos = markerList.get(lastIndex - 1).getPosition();
            } else { // ????????????
                // ???????????? ????????????????????????????????????
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
                // ????????????????????????
                for (int i = index; i < lastIndex; i++) {
                    view = LayoutInflater.from(this).inflate(R.layout.icon_marker, null);
                    ((TextView) view.findViewById(R.id.icon_text)).setText(String.valueOf(i + 1));
                    markerList.get(i).setIcon(BitmapDescriptorFactory.fromView(view));
                }
            }
            // ??????????????????null
            selectedMarker = null;
            // ????????????
            waypointList.remove(index);
            // ????????????????????????
            if (detailPanelVisible) {
                moveDetailPanel(300);
            }
        }
    }
}
