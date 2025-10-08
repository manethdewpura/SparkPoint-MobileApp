package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.app.DatePickerDialog;
import android.view.View;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.ead.sparkpoint.utils.LoadingDialog;
import com.ead.sparkpoint.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OperatorBookingsActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private TextView tvFilterDate;
    private View btnPickDate;

    private final List<JSONObject> bookings = new ArrayList<>();
    private BookingsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_bookings);

        progressBar = findViewById(R.id.progressBar);
        recyclerView = findViewById(R.id.recyclerView);
        tvEmpty = findViewById(R.id.tvEmpty);
        tvFilterDate = findViewById(R.id.tvFilterDate);
        btnPickDate = findViewById(R.id.btnPickDate);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BookingsAdapter(bookings);
        recyclerView.setAdapter(adapter);

        // Setup bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_operator_bookings);
        bottomNavigation.setOnItemSelectedListener(this);

        // Setup menu button
        setupMenuButton();

// Load today's bookings by default
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        tvFilterDate.setText("Bookings for " + today);
        btnPickDate.setOnClickListener(v -> openDatePicker(today));

// ✅ Add these lines for debugging
        AppUserDAO dao = new AppUserDAO(this);
        AppUser currentUser = dao.getUser();
        if (currentUser != null) {
            Log.d("TokenCheck", "AccessToken=" + currentUser.getAccessToken());
            Log.d("TokenCheck", "RoleId=" + currentUser.getRoleId());
        } else {
            Log.d("TokenCheck", "No user found in local DB");
        }

// Then fetch bookings
        fetchBookings(today);

    }

    private void openDatePicker(String current) {
        String[] parts = current.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        int day = Integer.parseInt(parts[2]);

        DatePickerDialog dpd = new DatePickerDialog(this, (view, y, m, d) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
            tvFilterDate.setText("Bookings for " + date);
            fetchBookings(date);
        }, year, month, day);
        dpd.getDatePicker().setMaxDate(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365);
        dpd.show();
    }

    private void fetchBookings(String date) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Loading bookings..."));

        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(OperatorBookingsActivity.this, Constants.GET_ALL_BOOKINGS_FOR_OPERATOR);

                if (response == null || response.isEmpty()) {
                    runOnUiThread(() -> {
                        loading.hide();
                        tvEmpty.setText("No data returned");
                        tvEmpty.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                JSONArray arr;
                try {
                    arr = new JSONArray(response);
                } catch (Exception parseEx) {
                    // Not an array; show error content briefly
                    runOnUiThread(() -> {
                        loading.hide();
                        tvEmpty.setText("Unexpected response");
                        tvEmpty.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                List<JSONObject> filtered = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String reservationTime = obj.optString("reservationTime", "");
                    // Simple filter by date (YYYY-MM-DD prefix)
                    if (reservationTime.startsWith(date)) {
                        filtered.add(obj);
                    }
                }

                runOnUiThread(() -> {
                    loading.hide();
                    if (filtered.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        bookings.clear();
                        bookings.addAll(filtered);
                        adapter.notifyDataSetChanged();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(this, "Failed to load bookings", Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private class BookingsAdapter extends RecyclerView.Adapter<BookingsAdapter.VH> {
        private final List<JSONObject> data;
        BookingsAdapter(List<JSONObject> data) { this.data = data; }

        class VH extends RecyclerView.ViewHolder {
            TextView tvTitle, tvSubtitle, tvStatus;
            VH(View itemView) {
                super(itemView);
                tvTitle = itemView.findViewById(R.id.tvTitle);
                tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
                tvStatus = itemView.findViewById(R.id.tvStatus);
                itemView.setOnClickListener(v -> onItemClicked(getBindingAdapterPosition()));
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            android.view.View view = getLayoutInflater().inflate(R.layout.item_operator_booking, parent, false);
            return new VH(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            JSONObject obj = data.get(position);
            String display = obj.optString("timeSlotDisplay", obj.optString("reservationTime", ""));
            String status = obj.optString("status", "");
            String ownerNIC = obj.optString("ownerNIC", "");
            holder.tvTitle.setText(display);
            holder.tvSubtitle.setText("NIC: " + ownerNIC);
            holder.tvStatus.setText(status);
        }

        @Override
        public int getItemCount() { return data.size(); }
    }

    private void onItemClicked(int position) {
        if (position < 0 || position >= bookings.size()) return;
        JSONObject obj = bookings.get(position);
        String bookingId = obj.optString("id", null);
        if (bookingId != null) {
            android.content.Intent intent = new android.content.Intent(this, BookingDetailActivity.class);
            intent.putExtra("bookingId", bookingId);
            startActivity(intent);
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_operator_home) {
            // Navigate back to home
            startActivity(new Intent(this, OperatorHomeActivity.class));
            finish();
            return true;
        } else if (itemId == R.id.nav_operator_bookings) {
            // Already on bookings; consume the event to keep highlight
            return true;
        } else if (itemId == R.id.nav_operator_profile) {
            startActivity(new android.content.Intent(this, ProfileActivity.class));
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

    @Override
    protected void onResume() {
        super.onResume();

        // optional: verify token validity or silently refresh
        new Thread(() -> {
            TokenManager tm = new TokenManager(this);
            String token = tm.getAccessToken();
            if (token == null || token.trim().isEmpty()) {
                tm.logoutUser();
            } else {
                // optional: silent refresh to ensure fresh token
                tm.refreshAccessToken();
            }
        }).start();
    }

}


