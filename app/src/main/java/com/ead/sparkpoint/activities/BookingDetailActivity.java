package com.ead.sparkpoint.activities;

import android.os.Bundle;
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

    TextView tvBookingInfo;
    Button btnStart, btnComplete;
    AppUser appUser;
    String bookingId;
    JSONObject bookingJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_detail);

        tvBookingInfo = findViewById(R.id.tvBookingInfo);
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
                String response = ApiClient.getRequest(Constants.GET_BOOKING_BY_ID_URL + bookingId, appUser.getAccessToken());
                bookingJson = new JSONObject(response);

                runOnUiThread(() -> {
                    try {
                        tvBookingInfo.setText(bookingJson.toString(2)); // simple display
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    String status = null;
                    try {
                        status = bookingJson.getString("status");
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    btnStart.setEnabled(status.equals("Confirmed"));
                    btnComplete.setEnabled(status.equals("In Progress"));
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

                String response = ApiClient.patchRequest(Constants.UPDATE_BOOKING_STATUS_URL + bookingId, req.toString(), appUser.getAccessToken());

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
