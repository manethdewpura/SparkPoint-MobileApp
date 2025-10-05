package com.ead.sparkpoint.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST_CODE = 101;

    TextView tvPending, tvConfirmed;
    GoogleMap mMap;
    FusedLocationProviderClient fusedLocationClient;
    double userLat, userLon;
    // Thread pool for background tasks
    ExecutorService executor = Executors.newFixedThreadPool(3);
    // Handler for UI updates
    Handler handler = new Handler(Looper.getMainLooper());

    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiI2OGQ2ODhmNzM5MGVjYzY0YjBmMTlmNWIiLCJ1bmlxdWVfbmFtZSI6InNhbmRpdGhpIiwiZW1haWwiOiJzYW5kaXRoaW5ldGhzaWx1bmlAZ21haWwuY29tIiwicm9sZSI6IjMiLCJuYmYiOjE3NTk1NTU3ODcsImV4cCI6MTc1OTU5MTc4NywiaWF0IjoxNzU5NTU1Nzg3LCJpc3MiOiJTcGFya1BvaW50X1NlcnZlciIsImF1ZCI6IlNwYXJrUG9pbnRfQ2xpZW50In0.SbGW0HT-zbxSJsMS8ul-PGWYTVJqeG9x935SmrR2UCo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvPending = findViewById(R.id.tvPendingCount);
        tvConfirmed = findViewById(R.id.tvConfirmedCount);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        //Button navigation to ReservationListActivity
        MaterialButton btnReservationList = findViewById(R.id.btnReservationList);
        btnReservationList.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, ReservationListActivity.class);
            startActivity(intent);
        });

        // Fetch counts
        fetchReservationCount(Constants.PENDING_BOOKINGS_URL, tvPending, "Pending Reservations: ");
        fetchReservationCount(Constants.UPCOMING_BOOKINGS_URL, tvConfirmed, "Future Reservations: ");

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        requestUserLocation();
    }

    private void requestUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                userLat = location.getLatitude();
                userLon = location.getLongitude();

                LatLng userLocation = new LatLng(userLat, userLon);
                mMap.addMarker(new MarkerOptions().position(userLocation).title("You are here"));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 12));

                // Fetch stations
                String url = Constants.GET_NEARBY_STATIONS_URL
                        + "&nearLoaction.longitude=" + userLon
                        + "&nearLoaction.latitude=" + userLat;

                fetchNearbyStations(url);
            }
        });
    }

    // Handle user permission result
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation();
        } else {
            Toast.makeText(this, "Location permission required to show stations.", Toast.LENGTH_LONG).show();
        }
    }

    // Fetch reservation counts (Pending / Confirmed)
    private void fetchReservationCount(String urlString, TextView textView, String label) {
        executor.execute(() -> {
            int count = 0;
            try {
                String response = ApiClient.getRequest(urlString, ACCESS_TOKEN);
                JSONArray jsonArray = new JSONArray(response);
                count = jsonArray.length();
            } catch (Exception e) {
                e.printStackTrace();
            }

            int finalCount = count;
            handler.post(() -> textView.setText(label + finalCount));
        });
    }

    // Fetch nearby stations and show on map
    private void fetchNearbyStations(String urlString) {
        executor.execute(() -> {
            try {
                String response = ApiClient.getRequest(urlString, ACCESS_TOKEN);
                JSONArray stationsArray = new JSONArray(response);

                handler.post(() -> {
                    try {
                        if (stationsArray.length() == 0) {
                            Toast.makeText(this, "No nearby stations found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        com.google.android.gms.maps.model.LatLngBounds.Builder builder =
                                new com.google.android.gms.maps.model.LatLngBounds.Builder();

                        // Include user location
                        LatLng userLocation = new LatLng(userLat, userLon);
                        builder.include(userLocation);

                        for (int i = 0; i < stationsArray.length(); i++) {
                            JSONObject station = stationsArray.getJSONObject(i);

                            // ✅ use "location" instead of "nearLocation"
                            JSONObject location = station.getJSONObject("location");
                            double lat = location.getDouble("latitude");
                            double lon = location.getDouble("longitude");
                            String name = station.getString("name");

                            LatLng stationLocation = new LatLng(lat, lon);
                            mMap.addMarker(new MarkerOptions()
                                    .position(stationLocation)
                                    .title(name)
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            );
                            builder.include(stationLocation);
                        }

                        // Auto-zoom to show all markers
                        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
