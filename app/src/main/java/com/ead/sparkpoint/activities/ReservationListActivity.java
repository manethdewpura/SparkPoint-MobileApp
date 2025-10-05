package com.ead.sparkpoint.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.ead.sparkpoint.R;
import com.ead.sparkpoint.adapters.ReservationAdapter;
import com.ead.sparkpoint.models.Reservation;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReservationListActivity extends AppCompatActivity {

    private RecyclerView recyclerReservations;
    private List<Reservation> reservationList;
    private ReservationAdapter adapter;
    private Button btnCurrent, btnUpcoming, btnPast;
    private FloatingActionButton btnAddReservation;
    private ActivityResultLauncher<Intent> reservationLauncher;

    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiI2OGQ2ODhmNzM5MGVjYzY0YjBmMTlmNWIiLCJ1bmlxdWVfbmFtZSI6InNhbmRpdGhpIiwiZW1haWwiOiJzYW5kaXRoaW5ldGhzaWx1bmlAZ21haWwuY29tIiwicm9sZSI6IjMiLCJuYmYiOjE3NTk1NTU3ODcsImV4cCI6MTc1OTU5MTc4NywiaWF0IjoxNzU5NTU1Nzg3LCJpc3MiOiJTcGFya1BvaW50X1NlcnZlciIsImF1ZCI6IlNwYXJrUG9pbnRfQ2xpZW50In0.SbGW0HT-zbxSJsMS8ul-PGWYTVJqeG9x935SmrR2UCo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation_list);

        btnCurrent = findViewById(R.id.btnCurrent);
        btnUpcoming = findViewById(R.id.btnUpcoming);
        btnPast = findViewById(R.id.btnPast);
        btnAddReservation = findViewById(R.id.btnAddReservation);

        recyclerReservations = findViewById(R.id.recyclerReservations);
        recyclerReservations.setLayoutManager(new LinearLayoutManager(this));

        reservationList = new ArrayList<>();
        adapter = new ReservationAdapter(reservationList, this, new ReservationAdapter.OnReservationActionListener() {
            @Override
            public void onViewQR(Reservation reservation) {
                // Launch QR activity
                Intent intent = new Intent(ReservationListActivity.this, QRCodeActivity.class);
                intent.putExtra("bookingId", reservation.getId());
                startActivity(intent);
            }

            @Override
            public void onUpdate(Reservation reservation) {
                // Launch ReservationActivity in update mode
                Intent intent = new Intent(ReservationListActivity.this, ReservationActivity.class);
                intent.putExtra("mode", "update");
                intent.putExtra("bookingId", reservation.getId());
                intent.putExtra("stationId", reservation.getStationId());
                intent.putExtra("stationName", reservation.getStationName());
                intent.putExtra("reservationTime", reservation.getReservationTime());
                intent.putExtra("slotsRequested", String.valueOf(reservation.getSlotsRequested()));
                reservationLauncher.launch(intent);
            }

            @Override
            public void onDelete(Reservation reservation) {
                // Calculate if reservation is at least 12 hours ahead
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    long reservationMillis = sdf.parse(reservation.getReservationTime()).getTime();
                    long nowMillis = System.currentTimeMillis();
                    long diffHours = (reservationMillis - nowMillis) / (1000 * 60 * 60);

                    if (diffHours < 12) {
                        Toast.makeText(ReservationListActivity.this,
                                "Reservation can only be cancelled at least 12 hours before.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Inflate custom layout
                    AlertDialog.Builder builder = new AlertDialog.Builder(ReservationListActivity.this);
                    View dialogView = getLayoutInflater().inflate(R.layout.dialog_delete_reservation, null);
                    builder.setView(dialogView);

                    TextView tvMessage = dialogView.findViewById(R.id.tvDialogMessage);
                    Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
                    Button btnConfirm = dialogView.findViewById(R.id.btnDialogConfirm);

                    tvMessage.setText("Do you want to cancel the following booking?\n\n" +
                            "Station: " + reservation.getStationName() + "\n" +
                            "Date & Time: " + reservation.getReservationTime() + "\n" +
                            "Slot: " + reservation.getReservationSlot());

                    AlertDialog dialog = builder.create();

                    btnCancel.setOnClickListener(v -> dialog.dismiss());
                    btnConfirm.setOnClickListener(v -> {
                        dialog.dismiss();
                        cancelReservation(reservation.getId());
                    });

                    dialog.show();

                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(ReservationListActivity.this, "Invalid reservation time", Toast.LENGTH_SHORT).show();
                }
            }
        });
        recyclerReservations.setAdapter(adapter);

        setActiveButton(btnCurrent);
        loadReservations(Constants.GET_BOOKINGS_URL );

        btnCurrent.setOnClickListener(v -> {
            setActiveButton(btnCurrent);
            loadReservations(Constants.GET_BOOKINGS_URL );
        });

        btnUpcoming.setOnClickListener(v -> {
            setActiveButton(btnUpcoming);
            loadReservations(Constants.UPCOMING_BOOKINGS_URL );
        });

        btnPast.setOnClickListener(v -> {
            setActiveButton(btnPast);
            loadReservations(Constants.PAST_BOOKINGS_URL );
        });

        reservationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        // Refresh reservations when booking was successful
                        loadReservations(Constants.GET_BOOKINGS_URL);
                    }
                }
        );

        btnAddReservation.setOnClickListener(v -> {
            Intent intent = new Intent(this, ReservationActivity.class);
            reservationLauncher.launch(intent);
        });

    }

    private void setActiveButton(Button activeButton) {
        btnCurrent.setSelected(false);
        btnUpcoming.setSelected(false);
        btnPast.setSelected(false);

        activeButton.setSelected(true);
    }

    private void loadReservations(String url) {
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(url, ACCESS_TOKEN);
                JSONArray arr = new JSONArray(response);

                reservationList.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    // Get nested station object
                    JSONObject stationObj = obj.getJSONObject("station");
                    String stationId = obj.getString("stationId");
                    String stationName = stationObj.getString("name");

                    // Get other fields
                    String reservationTime = obj.getString("reservationTime");
                    String reservationSlot = obj.getString("timeSlotDisplay");
                    int slotsRequested = obj.getInt("slotsRequested");
                    String status = obj.getString("status");
                    String id = obj.getString("id");

                    Reservation r = new Reservation(id, stationId, stationName, reservationTime, reservationSlot, slotsRequested, status);
                    reservationList.add(r);
                }

                runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error loading reservations", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void cancelReservation(String bookingId) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("status", "Cancelled");

                String url = Constants.DELETE_BOOKINGS_URL.replace("{bookingid}", bookingId);
                String response = ApiClient.patchRequest(url, body.toString(), ACCESS_TOKEN);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Reservation Cancelled!", Toast.LENGTH_SHORT).show();
                    loadReservations(Constants.GET_BOOKINGS_URL); // refresh list
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to cancel reservation", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}
