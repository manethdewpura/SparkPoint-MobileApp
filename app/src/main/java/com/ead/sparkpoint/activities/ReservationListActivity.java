package com.ead.sparkpoint.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
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
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.models.Reservation;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import android.widget.PopupMenu;
import com.ead.sparkpoint.utils.TokenManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReservationListActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private RecyclerView recyclerReservations;
    private List<Reservation> reservationList;
    private List<Reservation> allReservations;
    private ReservationAdapter adapter;
    private Button btnCurrent, btnUpcoming, btnPast;
    private ActivityResultLauncher<Intent> reservationLauncher;
    private String selectedStatusFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize bookings list screen, wire filters/nav, and load reservations
        super.onCreate(savedInstanceState);
        // redirect station operators away from EV owner bookings list
        AppUserDAO dao = new AppUserDAO(this);
        AppUser u = dao.getUser();
        if (u != null && Integer.valueOf(2).equals(u.getRoleId())) {
            startActivity(new Intent(this, OperatorBookingsActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_reservation_list);

        btnCurrent = findViewById(R.id.btnCurrent);
        btnUpcoming = findViewById(R.id.btnUpcoming);
        btnPast = findViewById(R.id.btnPast);

        // Setup bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        // Ensure the correct item is highlighted before wiring the listener
        bottomNavigation.setSelectedItemId(R.id.nav_bookings);
        bottomNavigation.setOnItemSelectedListener(this);
        
        // Setup menu button
        setupMenuButton();
        // Setup status filter button in this screen
        setupStatusFilterButton();

        recyclerReservations = findViewById(R.id.recyclerReservations);
        recyclerReservations.setLayoutManager(new LinearLayoutManager(this));

        reservationList = new ArrayList<>();
        allReservations = new ArrayList<>();
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
                // Enforce 12-hour rule before allowing update
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                    long reservationMillis = sdf.parse(reservation.getReservationTime()).getTime();
                    long nowMillis = System.currentTimeMillis();
                    long diffHours = (reservationMillis - nowMillis) / (1000 * 60 * 60);
                    if (diffHours < 12) {
                        Toast.makeText(ReservationListActivity.this,
                                "Reservation can only be updated at least 12 hours before.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

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
            String today = getTodayDate();
            String url = Constants.GET_BOOKINGS_URL + "?FromDate=" + today;
            loadReservations(url);
        });

        btnPast.setOnClickListener(v -> {
            setActiveButton(btnPast);
            String today = getTodayDate();
            String url = Constants.GET_BOOKINGS_URL + "?ToDate=" + today;
            loadReservations(url);
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

    }

    private void applyStatusFilter() {
        // Apply selected status to full list and refresh adapter
        reservationList.clear();
        if ("All".equalsIgnoreCase(selectedStatusFilter)) {
            reservationList.addAll(allReservations);
        } else {
            for (Reservation r : allReservations) {
                if (r.getStatus() != null && r.getStatus().equalsIgnoreCase(selectedStatusFilter)) {
                    reservationList.add(r);
                }
            }
        }
        adapter.notifyDataSetChanged();
        if (reservationList.isEmpty()) {
            Toast.makeText(this, "No reservations found", Toast.LENGTH_SHORT).show();
        }
    }

    private String getTodayDate() {
        // Return today's date formatted as yyyy-MM-dd for API filters
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }

    private void setActiveButton(Button activeButton) {
        // Update UI selected state for filter buttons
        btnCurrent.setSelected(false);
        btnUpcoming.setSelected(false);
        btnPast.setSelected(false);

        activeButton.setSelected(true);
    }

    private void loadReservations(String url) {
        // Fetch reservations from API, map to model list, and display
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(ReservationListActivity.this, url);
                JSONArray arr = new JSONArray(response);

                allReservations.clear();
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
                    allReservations.add(r);
                }

                runOnUiThread(this::applyStatusFilter);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error loading reservations", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void cancelReservation(String bookingId) {
        // Send cancellation request to API and refresh list on success
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("status", "Cancelled");

                String url = Constants.DELETE_BOOKINGS_URL.replace("{bookingid}", bookingId);
                String response = ApiClient.patchRequest(ReservationListActivity.this, url, body.toString());

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

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle bottom navigation taps and navigate between screens
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_bookings) {
            // Already on bookings screen
            return true;
        } else if (itemId == R.id.nav_home) {
            Intent homeIntent = new Intent(this, DashboardActivity.class);
            startActivity(homeIntent);
            return true;
        } else if (itemId == R.id.nav_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }
        
        return false;
    }

    private void setupMenuButton() {
        // Wire up the top app bar menu and handle logout action
        ImageButton menuButton = findViewById(R.id.menuButton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, menuButton);
                popup.getMenuInflater().inflate(R.menu.top_app_bar_menu, popup.getMenu());
                
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
    
    private void setupStatusFilterButton() {
        // Attach click listener to open the status filter dialog
        ImageButton btnFilter = findViewById(R.id.btnFilterStatus);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showStatusFilterDialog());
        }
    }

    private void showStatusFilterDialog() {
        // Show custom-styled filter dialog with single-choice statuses and actions
        final String[] statuses = new String[]{
                "All", "Pending", "Confirmed", "In Progress", "Completed", "Cancelled", "No Show"
        };
        int checkedIndex = 0;
        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(selectedStatusFilter)) {
                checkedIndex = i;
                break;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_status_filter, null);
        builder.setView(dialogView);

        android.widget.ListView listView = dialogView.findViewById(R.id.listStatuses);
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_single_choice,
                statuses
        );
        listView.setAdapter(adapter);
        listView.setChoiceMode(android.widget.ListView.CHOICE_MODE_SINGLE);
        listView.setItemChecked(checkedIndex, true);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedStatusFilter = statuses[position];
        });

        Button btnCancel = dialogView.findViewById(R.id.btnDialogCancel);
        Button btnApply = dialogView.findViewById(R.id.btnDialogApply);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            applyStatusFilter();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void logoutUser() {
        // Perform logout in background and finish the activity when done
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
