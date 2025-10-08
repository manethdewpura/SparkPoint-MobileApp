package com.ead.sparkpoint.activities;

import android.os.Bundle;
import android.view.View;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.chip.Chip;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.ead.sparkpoint.utils.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

public class BookingDetailActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    TextView tvBookingId, tvOwnerNIC, tvStationName, tvTimeSlot;
    Chip chipStatus;
    Button btnStart, btnComplete;
    AppUser appUser;
    String bookingId;
    JSONObject bookingJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // Initialize UI
        tvBookingId = findViewById(R.id.tvBookingId);
        tvOwnerNIC = findViewById(R.id.tvOwnerNIC);
        tvStationName = findViewById(R.id.tvStationName);
        tvTimeSlot = findViewById(R.id.tvTimeSlot);
        chipStatus = findViewById(R.id.chipStatus);
        btnStart = findViewById(R.id.btnStart);
        btnComplete = findViewById(R.id.btnComplete);

        bookingId = getIntent().getStringExtra("bookingId");

        appUser = new AppUserDAO(this).getUser();

        loadBookingDetails();

        btnStart.setOnClickListener(v -> updateBookingStatus("In Progress"));
        btnComplete.setOnClickListener(v -> updateBookingStatus("Completed"));

        // Setup bottom navigation for Station Operator context
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_operator_bookings);
            bottomNavigation.setOnItemSelectedListener(this);
        }
    }

    private void loadBookingDetails() {
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Loading booking..."));
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(BookingDetailActivity.this, Constants.GET_BOOKING_BY_ID_URL + bookingId);
                bookingJson = new JSONObject(response);

                runOnUiThread(() -> {
                    loading.hide();
                    try {
                        // Populate fields
                        tvBookingId.setText(bookingJson.optString("id"));
                        tvOwnerNIC.setText(bookingJson.optString("ownerNIC"));
                        if (bookingJson.has("station")) {
                            tvStationName.setText(bookingJson.getJSONObject("station").optString("name"));
                        }
                        tvTimeSlot.setText(bookingJson.optString("timeSlotDisplay"));
                        String statusText = bookingJson.optString("status");
                        chipStatus.setText(statusText);
                        styleStatusChip(statusText);

                        // Handle button visibility based on status
                        String status = bookingJson.optString("status", "");

                        if ("Confirmed".equalsIgnoreCase(status)) {
                            btnStart.setVisibility(View.VISIBLE);
                            btnComplete.setVisibility(View.GONE);
                        } else if ("In Progress".equalsIgnoreCase(status)) {
                            btnStart.setVisibility(View.GONE);
                            btnComplete.setVisibility(View.VISIBLE);
                        } else {
                            // Completed, Cancelled, or any other status
                            btnStart.setVisibility(View.GONE);
                            btnComplete.setVisibility(View.GONE);
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error parsing booking details", Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                        loading.hide();
                        Toast.makeText(this, "Error loading booking: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void updateBookingStatus(String newStatus) {
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Updating booking..."));
        new Thread(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("OwnerNIC", bookingJson.getString("ownerNIC"));
                req.put("StationId", bookingJson.getString("stationId"));
                req.put("ReservationTime", bookingJson.getString("reservationTime"));
                req.put("Status", newStatus);

                String response = ApiClient.patchRequest(BookingDetailActivity.this, Constants.UPDATE_BOOKING_STATUS_URL + bookingId, req.toString());

                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    loadBookingDetails(); // refresh
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                        loading.hide();
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void styleStatusChip(String status) {
        int bgColor;
        if ("Confirmed".equalsIgnoreCase(status)) {
            bgColor = getResources().getColor(R.color.light_blue);
        } else if ("In Progress".equalsIgnoreCase(status)) {
            bgColor = getResources().getColor(R.color.orange);
        } else if ("Completed".equalsIgnoreCase(status)) {
            bgColor = getResources().getColor(R.color.success_green);
        } else if ("Cancelled".equalsIgnoreCase(status)) {
            bgColor = getResources().getColor(R.color.error_red);
        } else {
            bgColor = getResources().getColor(R.color.light_blue);
        }
        chipStatus.setChipBackgroundColorResource(android.R.color.transparent);
        chipStatus.setTextColor(getResources().getColor(android.R.color.white));
        chipStatus.setChipBackgroundColor(android.content.res.ColorStateList.valueOf(bgColor));
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_operator_home) {
            startActivity(new android.content.Intent(this, OperatorHomeActivity.class));
            return true;
        } else if (itemId == R.id.nav_operator_bookings) {
            // Stay on bookings-related screen
            return true;
        } else if (itemId == R.id.nav_operator_profile) {
            startActivity(new android.content.Intent(this, ProfileActivity.class));
            return true;
        }
        return false;
    }
}
