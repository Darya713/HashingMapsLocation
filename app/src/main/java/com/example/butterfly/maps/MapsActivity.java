package com.example.butterfly.maps;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    GoogleMap mMap;
    GPSTracker gps;
    double latitude, longitude;
    EditText timer;
    Button getDeviceId, enter;
    ImageButton button;
    static File directory;
    int timerLocation;
    String device_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        createDirectory();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        gps = new GPSTracker(MapsActivity.this);
        timer = (EditText) findViewById(R.id.timer);
        getDeviceId = (Button) findViewById(R.id.device_id);
        enter = (Button) findViewById(R.id.enter);
        button = (ImageButton) findViewById(R.id.button2);

        device_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        enter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timerLocation = Integer.parseInt(timer.getText().toString());
                ServerParams serverParams = new ServerParams(getApplicationContext(), device_id, timerLocation);
                Server server = new Server(getBaseContext());
                server.execute(serverParams);
            }
        });

        getDeviceId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MapsActivity.this, device_id, Toast.LENGTH_SHORT).show();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gps = new GPSTracker(MapsActivity.this);
                if (gps.canGetLocation()) {
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                    Toast.makeText(getBaseContext(), "Latitude: " + latitude + "\nLongitude: " + longitude, Toast.LENGTH_SHORT).show();
                } else {
                    gps.showSettingsAlert();
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        if (gps.canGetLocation()) {
            latitude = gps.getLatitude();
            longitude = gps.getLongitude();
        } else {
            gps.showSettingsAlert();
        }

        mMap = googleMap;
        LatLng location = new LatLng(latitude, longitude);
        mMap.addMarker(new MarkerOptions().position(location).title("Marker in Minsk"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15));

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);
    }

    private void createDirectory() {
        directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyFolder");

        if (!directory.exists())
            directory.mkdirs();
    }
}
