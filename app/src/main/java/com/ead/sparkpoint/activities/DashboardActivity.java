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
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.ead.sparkpoint.utils.LoadingDialog;
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
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import com.ead.sparkpoint.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DashboardActivity extends AppCompatActivity implements OnMapReadyCallback, NavigationBarView.OnItemSelectedListener {

    private static final int LOCATION_REQUEST_CODE = 101;

    TextView tvPending, tvConfirmed, tvWelcomeUser, tvUserEmail, tvUserNic;
    GoogleMap mMap;
    FusedLocationProviderClient fusedLocationClient;
    double userLat, userLon;
    // Thread pool for background tasks
    ExecutorService executor = Executors.newFixedThreadPool(3);
    // Handler for UI updates
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Set up dashboard screen, redirect operators, initialize UI and data
        super.onCreate(savedInstanceState);
        //redirect station operators away from EV owner dashboard
        AppUserDAO dao = new AppUserDAO(this);
        AppUser u = dao.getUser();
        if (u != null && Integer.valueOf(2).equals(u.getRoleId())) {
            startActivity(new Intent(this, OperatorHomeActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_dashboard);


        tvPending = findViewById(R.id.tvPendingCount);
        tvConfirmed = findViewById(R.id.tvConfirmedCount);
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser);
        tvUserEmail = findViewById(R.id.tvUserEmail);
        tvUserNic = findViewById(R.id.tvUserNic);

        // Load and display user information
        loadUserInfo();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Setup bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        // Ensure the correct item is highlighted before wiring the listener
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        bottomNavigation.setOnItemSelectedListener(this);
        
        // Setup menu button
        setupMenuButton();

        // Fetch counts
        fetchReservationCount(Constants.PENDING_BOOKINGS_URL, tvPending);
        fetchReservationCount(Constants.UPCOMING_BOOKINGS_URL, tvConfirmed);

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // View Available Stations button
        MaterialButton btnViewStations = findViewById(R.id.btnViewStations);
        if (btnViewStations != null) {
            btnViewStations.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, StationListActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Receive map instance and kick off location request
        mMap = googleMap;
        requestUserLocation();
    }

    private void requestUserLocation() {
        // Request user location permission and center map on user when available
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

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // Handle result of location permission request and proceed accordingly
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST_CODE &&
                grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestUserLocation();
        } else {
            Toast.makeText(this, "Location permission required to show stations.", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchReservationCount(String urlString, TextView textView) {
        // Fetch reservation count from API in background and update provided TextView
        executor.execute(() -> {
            int count = 0;
            try {
                String response = ApiClient.getRequest(DashboardActivity.this, urlString);
                JSONArray jsonArray = new JSONArray(response);
                count = jsonArray.length();
            } catch (Exception e) {
                e.printStackTrace();
            }

            int finalCount = count;
            handler.post(() -> textView.setText(String.valueOf(finalCount)));
        });
    }

    private void fetchNearbyStations(String urlString) {
        // Fetch nearby stations and render markers on the map
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Loading nearby stations..."));
        executor.execute(() -> {
            try {
                String response = ApiClient.getRequest(DashboardActivity.this, urlString);
                JSONArray stationsArray = new JSONArray(response);

                handler.post(() -> {
                    try {
                        loading.hide();
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
                handler.post(loading::hide);
            }
        });
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle bottom navigation item taps and navigate between screens
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            // Already on home screen
            return true;
        } else if (itemId == R.id.nav_bookings) {
            Intent bookingsIntent = new Intent(this, ReservationListActivity.class);
            startActivity(bookingsIntent);
            return true;
        } else if (itemId == R.id.nav_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }
        
        return false;
    }

    private void setupMenuButton() {
        // Wire up the top app bar menu and handle menu actions
        ImageButton menuButton = findViewById(R.id.menuButton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, menuButton);
                popup.getMenuInflater().inflate(R.menu.top_app_bar_menu, popup.getMenu());
                
                // Ensure icons are shown in the popup
                try {
                    java.lang.reflect.Field mFieldPopup = PopupMenu.class.getDeclaredField("mPopup");
                    mFieldPopup.setAccessible(true);
                    Object mPopup = mFieldPopup.get(popup);
                    mPopup.getClass().getDeclaredMethod("setForceShowIcon", boolean.class)
                            .invoke(mPopup, true);
                } catch (Exception ignored) { }

                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_logout) {
                        logoutUser();
                        return true;
                    }
                    return false;
                });
                
                popup.show();
            });
        }
    }

    private void loadUserInfo() {
        // Load user details from local DB and populate header fields
        AppUserDAO dao = new AppUserDAO(this);
        AppUser user = dao.getUser();

        if (user != null) {
            tvWelcomeUser.setText("Welcome " + user.getUsername() + "!");
            tvUserEmail.setText("Email: " + user.getEmail());
            tvUserNic.setText("NIC: " + user.getNic());
        }
    }

    private void logoutUser() {
        // Perform logout in background and finish the activity on completion
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Signing out..."));
        new Thread(() -> {
            try {
                TokenManager tokenManager = new TokenManager(this);
                tokenManager.logoutUser();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loading.hide();
                    finish();
                });
                return;
            }
            runOnUiThread(loading::hide);
        }).start();
    }
}
