package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.LoadingDialog;
import com.ead.sparkpoint.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class OperatorHomeActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    TextView tvWelcomeOperator, tvOperatorEmail;
    Button btnScanQR;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_home);

        tvWelcomeOperator = findViewById(R.id.tvWelcomeOperator);
        tvOperatorEmail = findViewById(R.id.tvOperatorEmail);
        View btnScanQR = findViewById(R.id.btnScanQR);

        AppUserDAO dao = new AppUserDAO(this);
        AppUser user = dao.getUser();

        if (user != null) {
            tvWelcomeOperator.setText("Welcome Operator " + user.getUsername());
            tvOperatorEmail.setText("Email: " + user.getEmail());
        }

        btnScanQR.setOnClickListener(v -> startActivity(new Intent(OperatorHomeActivity.this, ScanBookingActivity.class)));

        // Setup bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_operator_home);
        bottomNavigation.setOnItemSelectedListener(this);

        // Setup menu button
        setupMenuButton();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_operator_home) {
            // Already on home; consume the event to keep highlight
            return true;
        } else if (itemId == R.id.nav_operator_bookings) {
            startActivity(new Intent(this, OperatorBookingsActivity.class));
            return true;
        } else if (itemId == R.id.nav_operator_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
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
