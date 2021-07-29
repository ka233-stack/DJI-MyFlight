package com.dji.myFlight;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;


public class ManualFlightActivity extends AppCompatActivity implements View.OnClickListener {

    private Button mMediaManagerBtn;
    private MapView mapView;
    private AMap aMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_flight);

        mapView = findViewById(R.id.mapWidget);
        mapView.onCreate(savedInstanceState);


        initMapView();
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

    private void initMapView() {

        if (aMap == null) {
            aMap = mapView.getMap();
            // aMap.setOnMapClickListener(this);// add the listener for click for amap object
        }
        aMap.moveCamera(CameraUpdateFactory.zoomTo(18));

    }
}