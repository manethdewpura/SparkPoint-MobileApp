package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    private BottomNavigationView bottomNavigationView;
    private static final String HOME_TAG = "home";
    private static final String BOOKINGS_TAG = "bookings";
    private static final String PROFILE_TAG = "profile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication first
        if (!isUserAuthenticated()) {
            // Redirect to login if not authenticated
            Intent loginIntent = new Intent(this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
            return;
        }
        
        // User is authenticated, proceed with normal flow
        // Route by role: if Station Operator (roleId == 2), go to operator home
        AppUserDAO userDAO = new AppUserDAO(this);
        AppUser currentUser = userDAO.getUser();
        if (currentUser != null && Integer.valueOf(2).equals(currentUser.getRoleId())) {
            Intent operatorHome = new Intent(this, OperatorHomeActivity.class);
            operatorHome.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(operatorHome);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        // Find the bottom navigation view
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            // Set default selection before attaching listener
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
            bottomNavigationView.setOnItemSelectedListener(this);
        }
        
        // Setup menu button
        setupMenuButton();
        
        // Load home fragment by default
        loadFragment(HOME_TAG);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            loadFragment(HOME_TAG);
            return true;
        } else if (itemId == R.id.nav_bookings) {
            loadFragment(BOOKINGS_TAG);
            return true;
        } else if (itemId == R.id.nav_profile) {
            loadFragment(PROFILE_TAG);
            return true;
        }
        
        return false;
    }

    private void loadFragment(String tag) {
        Fragment fragment = null;
        
        switch (tag) {
            case HOME_TAG:
                // Navigate to DashboardActivity
                Intent dashboardIntent = new Intent(this, DashboardActivity.class);
                startActivity(dashboardIntent);
                return;
                
            case BOOKINGS_TAG:
                // Navigate to ReservationListActivity
                Intent bookingsIntent = new Intent(this, ReservationListActivity.class);
                startActivity(bookingsIntent);
                return;
                
            case PROFILE_TAG:
                // Navigate to ProfileActivity
                Intent profileIntent = new Intent(this, ProfileActivity.class);
                startActivity(profileIntent);
                return;
        }
    }
    
    /**
     * Check if user is authenticated by verifying if there's a valid user session
     * @return true if user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated() {
        AppUserDAO userDAO = new AppUserDAO(this);
        AppUser user = userDAO.getUser();
        
        // Check if user exists and has access token
        if (user == null || user.getAccessToken() == null || user.getAccessToken().isEmpty()) {
            return false;
        }
        
        // Optional: You can add additional token validation here
        // For now, we'll just check if the token exists
        return true;
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
     * Clears local database and calls logout API
     */
    private void logoutUser() {
        new Thread(() -> {
            try {
                // Call logout API
                TokenManager tokenManager = new TokenManager(this);
                tokenManager.logoutUser(); // This method already handles API call and local DB clearing
                
                runOnUiThread(() -> {
                    // The TokenManager.logoutUser() already redirects to LoginActivity
                    // So we don't need to do anything here
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    // Even if API call fails, clear local data and redirect
                    AppUserDAO userDAO = new AppUserDAO(this);
                    userDAO.clearUsers();
                    
                    Intent loginIntent = new Intent(this, LoginActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(loginIntent);
                    finish();
                });
            }
        }).start();
    }
}
