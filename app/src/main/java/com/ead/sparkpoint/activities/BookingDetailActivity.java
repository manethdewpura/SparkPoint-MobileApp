package com.ead.sparkpoint.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;

import org.json.JSONException;
import org.json.JSONObject;

public class BookingDetailActivity extends AppCompatActivity {

    TextView tvBookingId, tvOwnerNIC, tvStationName, tvTimeSlot, tvStatus;
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
        tvStatus = findViewById(R.id.tvStatus);
        btnStart = findViewById(R.id.btnStart);
        btnComplete = findViewById(R.id.btnComplete);

        bookingId = getIntent().getStringExtra("bookingId");

        appUser = new AppUserDAO(this).getUser();

        loadBookingDetails();

        btnStart.setOnClickListener(v -> updateBookingStatus("In Progress"));
        btnComplete.setOnClickListener(v -> updateBookingStatus("Completed"));
    }

    private void loadBookingDetails() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(BookingDetailActivity.this, Constants.GET_BOOKING_BY_ID_URL + bookingId);
                bookingJson = new JSONObject(response);

                runOnUiThread(() -> {
                    try {
                        // Populate fields
                        tvBookingId.setText("Booking ID: " + bookingJson.optString("id"));
                        tvOwnerNIC.setText("Owner NIC: " + bookingJson.optString("ownerNIC"));
                        if (bookingJson.has("station")) {
                            tvStationName.setText("Station: " + bookingJson.getJSONObject("station").optString("name"));
                        }
                        tvTimeSlot.setText("Time Slot: " + bookingJson.optString("timeSlotDisplay"));
                        tvStatus.setText("Status: " + bookingJson.optString("status"));

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
                runOnUiThread(() ->
                        Toast.makeText(this, "Error loading booking: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    private void updateBookingStatus(String newStatus) {
        new Thread(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("OwnerNIC", bookingJson.getString("ownerNIC"));
                req.put("StationId", bookingJson.getString("stationId"));
                req.put("ReservationTime", bookingJson.getString("reservationTime"));
                req.put("Status", newStatus);

                String response = ApiClient.patchRequest(BookingDetailActivity.this, Constants.UPDATE_BOOKING_STATUS_URL + bookingId, req.toString());

                runOnUiThread(() -> {
                    Toast.makeText(this, response, Toast.LENGTH_SHORT).show();
                    loadBookingDetails(); // refresh
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }
}
