package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.adapters.StationAdapter;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.ead.sparkpoint.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StationListActivity extends AppCompatActivity implements StationAdapter.OnStationActionListener, NavigationBarView.OnItemSelectedListener {

    private RecyclerView recyclerStations;
    private StationAdapter adapter;
    private final List<StationAdapter.StationItem> stations = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_list);

        recyclerStations = findViewById(R.id.recyclerStations);
        recyclerStations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StationAdapter(stations, this);
        recyclerStations.setAdapter(adapter);

        // Setup bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_bookings);
        bottomNavigation.setOnItemSelectedListener(this);
        
        // Setup menu button
        setupMenuButton();

        loadStations();
    }

    private void loadStations() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(StationListActivity.this, Constants.GET_NEARBY_STATIONS_URL);
                JSONArray arr = new JSONArray(response);
                stations.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    String address = obj.optString("address", "");
                    String phone = obj.optString("contactPhone", "");
                    stations.add(new StationAdapter.StationItem(id, name, address, phone));
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load stations", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onAddBooking(StationAdapter.StationItem station) {
        Intent intent = new Intent(this, ReservationActivity.class);
        intent.putExtra("lockStation", true);
        intent.putExtra("stationId", station.id);
        intent.putExtra("stationName", station.name);
        startActivity(intent);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            Intent homeIntent = new Intent(this, DashboardActivity.class);
            startActivity(homeIntent);
            return true;
        } else if (itemId == R.id.nav_bookings) {
            // Already on bookings; consume the event to keep highlight
            return true;
        } else if (itemId == R.id.nav_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }
        
        return false;
    }
    
    /**
     * Setup the menu button in the top app bar
     */
    private void setupMenuButton() {
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
    
    /**
     * Logout user from the app
     */
    private void logoutUser() {
        new Thread(() -> {
            try {
                TokenManager tokenManager = new TokenManager(this);
                tokenManager.logoutUser();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    finish();
                });
            }
        }).start();
    }
}



