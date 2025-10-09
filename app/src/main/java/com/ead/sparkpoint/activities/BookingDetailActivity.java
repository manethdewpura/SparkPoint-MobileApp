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

//    Called when the activity is first created.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        // Initialize UI components by finding them in the layout
        tvBookingId = findViewById(R.id.tvBookingId);
        tvOwnerNIC = findViewById(R.id.tvOwnerNIC);
        tvStationName = findViewById(R.id.tvStationName);
        tvTimeSlot = findViewById(R.id.tvTimeSlot);
        chipStatus = findViewById(R.id.chipStatus);
        btnStart = findViewById(R.id.btnStart);
        btnComplete = findViewById(R.id.btnComplete);

        //Retrieve the booking ID passed from the previous activity.
        bookingId = getIntent().getStringExtra("bookingId");
        appUser = new AppUserDAO(this).getUser();

        loadBookingDetails();

        // Set listeners for the start and complete buttons to update the booking status.
        btnStart.setOnClickListener(v -> updateBookingStatus("In Progress"));
        btnComplete.setOnClickListener(v -> updateBookingStatus("Completed"));

        // Setup bottom navigation for Station Operator context
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_operator_bookings);
            bottomNavigation.setOnItemSelectedListener(this);
        }
    }

    /**
     * Fetches the booking details from the API in a background thread.
     * Once fetched, it updates the UI on the main thread.
     */
    private void loadBookingDetails() {
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Loading booking..."));
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(BookingDetailActivity.this, Constants.GET_BOOKING_BY_ID_URL + bookingId);
                bookingJson = new JSONObject(response);

                // Update UI elements on the main thread after the network request is complete.
                runOnUiThread(() -> {
                    loading.hide();
                    try {
                        // Populate UI fields with data from the JSON response.
                        tvBookingId.setText(bookingJson.optString("id"));
                        tvOwnerNIC.setText(bookingJson.optString("ownerNIC"));
                        if (bookingJson.has("station")) {
                            tvStationName.setText(bookingJson.getJSONObject("station").optString("name"));
                        }
                        tvTimeSlot.setText(bookingJson.optString("timeSlotDisplay"));
                        String statusText = bookingJson.optString("status");
                        chipStatus.setText(statusText);
                        styleStatusChip(statusText);

                        // Handle button visibility of action buttons based on current status
                        String status = bookingJson.optString("status", "");

                        if ("Confirmed".equalsIgnoreCase(status)) {
                            btnStart.setVisibility(View.VISIBLE);
                            btnComplete.setVisibility(View.GONE);
                        } else if ("In Progress".equalsIgnoreCase(status)) {
                            btnStart.setVisibility(View.GONE);
                            btnComplete.setVisibility(View.VISIBLE);
                        } else {
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

    /**
     * Updates the status of the current booking by sending a PATCH request to the API.
     */
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

                // After updating, show a confirmation and refresh the booking details.
                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    loadBookingDetails(); // refresh the view with the latest data
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

    /**
     * Styles the status chip with a specific background color based on the booking status.
     */
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

    /**
     * Handles navigation item selections in the bottom navigation bar.
     * @param item The selected menu item.
     * @return True to display the item as the selected item, false otherwise.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_operator_home) {
            startActivity(new android.content.Intent(this, OperatorHomeActivity.class));
            return true;
        } else if (itemId == R.id.nav_operator_bookings) {
            // Stay on this bookings screen
            return true;
        } else if (itemId == R.id.nav_operator_profile) {
            startActivity(new android.content.Intent(this, ProfileActivity.class));
            return true;
        }
        return false;
    }
}
