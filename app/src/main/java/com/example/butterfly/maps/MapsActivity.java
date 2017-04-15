package com.example.butterfly.maps;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.audiofx.BassBoost;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DirectionFinderListener {
    public static double latitude, longitude, mapsSpeed;
    private GoogleMap mMap;
    EditText start, finish;
    TextView distance, timer, speed;
    ImageButton route, location;
    Button hash;

    File directory;

    GPSTracker gps;
    private List<Marker> originMarkers = new ArrayList<>();
    private List<Marker> destinationMarkers = new ArrayList<>();
    private List<Polyline> polylinePaths = new ArrayList<>();
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        createDirectory();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        start = (EditText) findViewById(R.id.start);
        finish = (EditText) findViewById(R.id.finish);
        distance = (TextView) findViewById(R.id.distance);
        timer = (TextView) findViewById(R.id.timer);
        speed = (TextView) findViewById(R.id.speed);
        route = (ImageButton) findViewById(R.id.route);
        location = (ImageButton) findViewById(R.id.location);
        hash = (Button) findViewById(R.id.hash);

        route.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (start.getText().toString().isEmpty())
                    showToast("Введите точку отправления");
                if (finish.getText().toString().isEmpty())
                    showToast("Введите точку назначения");
                try {
                    new DirectionFinder(MapsActivity.this, start.getText().toString(), finish.getText().toString()).execute();
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        });

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gps = new GPSTracker(MapsActivity.this);
                if (gps.canGetLocation()) {
//                    latitude = 123.123;
//                    longitude = 12.0755;
//                    mapsSpeed = 4.7;
                    latitude = gps.getLatitude();
                    longitude = gps.getLongitude();
                    mapsSpeed = gps.getSpeed();
                    showToast("Latitude: " + latitude + "\nLongitude: " + longitude + "\nSpeed: " + mapsSpeed);
//                    showToast("Latitude: 123.123");
                } else {
                    gps.showSettingsAlert();
                }
                Intent intent = new Intent(MapsActivity.this, MapsActivity.class);
                startActivity(intent);
                MapsActivity.this.finish();
            }
        });

        hash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MapsActivity.this, "Hash", Toast.LENGTH_SHORT).show();
                try {
                    Intent hashIntent = new Intent(MapsActivity.this, HashActivity.class);
                    long start = System.currentTimeMillis();
                    String location = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID) + ", " +
                            latitude + ", " + longitude;
                    try {
                        OutputStream os = new FileOutputStream(directory.getPath() + "/" + "hash.txt");
                        byte[] data = location.getBytes();
                        os.write(data);
                        os.close();
                    } catch (IOException e){
                        Log.d("ERROR", e.getMessage());
                    }
                    String temp = new MyCryptography().getHash(directory.getPath() + "/" + "hash.txt");
                    long stop = System.currentTimeMillis();
                    hashIntent.putExtra("UID", Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
                    hashIntent.putExtra("location", location);
                    hashIntent.putExtra("hash", temp);
                    hashIntent.putExtra("start", String.valueOf(start));
                    hashIntent.putExtra("stop", String.valueOf(stop));
                    startActivity(hashIntent);
                }
                catch (Throwable e) {
                    Log.d("Error", e.getMessage());
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
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

    @Override
    public void onDirectionFinderStart() {
        progressDialog = ProgressDialog.show(this, "Please wait.",
                "Finding direction!..", true);

        if (originMarkers != null) {
            for (Marker marker : originMarkers) {
                marker.remove();
            }
        }

        if (destinationMarkers != null) {
            for (Marker marker : destinationMarkers) {
                marker.remove();
            }
        }

        if (polylinePaths != null) {
            for (Polyline polyline:polylinePaths ) {
                polyline.remove();
            }
        }
    }

    @Override
    public void onDirectionFinderSuccess(List<Route> routes) {
        progressDialog.dismiss();
        polylinePaths = new ArrayList<>();
        originMarkers = new ArrayList<>();
        destinationMarkers = new ArrayList<>();

        for (Route route : routes) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.startLocation, 16));
            timer.setText(route.duration.text);
            distance.setText(route.distance.text);

            originMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_start))
                    .title(route.startAddress)
                    .position(route.startLocation)));
            destinationMarkers.add(mMap.addMarker(new MarkerOptions()
                    .icon(BitmapDescriptorFactory.fromResource(R.mipmap.ic_finish))
                    .title(route.endAddress)
                    .position(route.endLocation)));

            PolylineOptions polylineOptions = new PolylineOptions().
                    geodesic(true).
                    color(Color.BLUE).
                    width(10);

            for (int i = 0; i < route.points.size(); i++)
                polylineOptions.add(route.points.get(i));

            polylinePaths.add(mMap.addPolyline(polylineOptions));
        }
    }

    public void showToast(String message) {
        Toast.makeText(this.getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void createDirectory() {
        directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),"MyFolder");
//        getFilesDir().getAbsolutePath()
        if (!directory.exists())
            directory.mkdirs();
    }
}
