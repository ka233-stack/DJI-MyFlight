package com.dji.myFlight;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class ManualFlightActivity extends AppCompatActivity implements View.OnClickListener{

    private Button btnGallery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_flight);
        btnGallery = (Button)findViewById(R.id.btn_gallery);
        btnGallery.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_gallery) {
            Intent intent = new Intent(this, GalleryActivity.class);
            startActivity(intent);
        }
    }
}