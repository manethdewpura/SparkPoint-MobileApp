package com.ead.sparkpoint.activities;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;

import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.Intent;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import com.ead.sparkpoint.utils.TokenManager;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


public class ProfileActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    EditText etUsername, etEmail, etFirstName, etLastName, etPassword, etNic, etPhone;
    Button btnEdit, btnSave;
    AppUserDAO appUserDAO;
    AppUser appUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPassword = findViewById(R.id.etPassword);
        etNic = findViewById(R.id.etNic);
        etPhone = findViewById(R.id.etPhone);
        btnEdit = findViewById(R.id.btnEdit);
        btnSave = findViewById(R.id.btnSave);

        Button btnDeactivate = findViewById(R.id.btnDeactivate);
        btnDeactivate.setOnClickListener(v -> showDeactivateDialog());

        // Setup bottom navigation (EV Owner menu by default). If operator, we switch menu.
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        AppUserDAO daoForRole = new AppUserDAO(this);
        AppUser roleUser = daoForRole.getUser();
        if (roleUser != null && Integer.valueOf(2).equals(roleUser.getRoleId())) {
            bottomNavigation.getMenu().clear();
            bottomNavigation.inflateMenu(R.menu.bottom_navigation_operator_menu);
            bottomNavigation.setSelectedItemId(R.id.nav_operator_profile);
        } else {
            bottomNavigation.setSelectedItemId(R.id.nav_profile);
        }
        bottomNavigation.setOnItemSelectedListener(this);

        // Setup menu button
        setupMenuButton();

        appUserDAO = new AppUserDAO(this);
        appUser = appUserDAO.getUser(); // load from local db

        // Fill data
        if (appUser != null) {
            etUsername.setText(appUser.getUsername());
            etEmail.setText(appUser.getEmail());
            etFirstName.setText(appUser.getFirstName());
            etLastName.setText(appUser.getLastName());
            etPassword.setText(appUser.getPassword());
            etNic.setText(appUser.getNic());
            etPhone.setText(appUser.getPhone());
        }

        // Default: fields are read-only until user taps Edit
        boolean isOperator = roleUser != null && Integer.valueOf(2).equals(roleUser.getRoleId());
        setEditable(false);
        if (isOperator) {
            // disable inputs explicitly for safety
            etUsername.setEnabled(false);
            etEmail.setEnabled(false);
            etFirstName.setEnabled(false);
            etLastName.setEnabled(false);
            etPassword.setEnabled(false);
            etNic.setEnabled(false);
            etPhone.setEnabled(false);

            // Completely hide Edit, Save, and Deactivate buttons
            btnEdit.setVisibility(android.view.View.GONE);
            btnSave.setVisibility(android.view.View.GONE);
            findViewById(R.id.btnDeactivate).setVisibility(android.view.View.GONE);

            // Hide NIC and Phone fields for station operator
            android.view.View tlNic = findViewById(R.id.tlNic);
            android.view.View tlPhone = findViewById(R.id.tlPhone);
            if (tlNic != null) tlNic.setVisibility(android.view.View.GONE);
            if (tlPhone != null) tlPhone.setVisibility(android.view.View.GONE);
        }

        btnEdit.setOnClickListener(v -> {
            setEditable(true);
            btnEdit.setVisibility(android.view.View.GONE);
            btnSave.setVisibility(android.view.View.VISIBLE);
        });

        btnSave.setOnClickListener(v -> {
            updateProfile();
            btnEdit.setVisibility(android.view.View.VISIBLE);
            btnSave.setVisibility(android.view.View.GONE);
        });

    }

    private void setEditable(boolean enabled) {
        etUsername.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etFirstName.setEnabled(enabled);
        etLastName.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        // NIC should not be editable (PK)
        etNic.setEnabled(false);
        etPhone.setEnabled(enabled);

        btnSave.setEnabled(enabled);
    }

    private void updateProfile() {
        String username = etUsername.getText().toString();
        String email = etEmail.getText().toString();
        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();
        String password = etPassword.getText().toString();
        String nic = etNic.getText().toString();
        String phone = etPhone.getText().toString();

        new Thread(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("Username", username);
                req.put("Email", email);
                req.put("FirstName", firstName);
                req.put("LastName", lastName);
                req.put("Password", password);   // ✅ always include
                req.put("NIC", nic);
                req.put("Phone", phone);

                String response = ApiClient.patchRequest(
                        ProfileActivity.this,
                        Constants.UPDATE_EV_OWNER_URL,
                        req.toString()
                );

                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, response, Toast.LENGTH_SHORT).show();

                    // update local DB
                    appUser.setUsername(username);
                    appUser.setEmail(email);
                    appUser.setFirstName(firstName);
                    appUser.setLastName(lastName);
                    appUser.setPassword(password);
                    appUser.setNic(nic);
                    appUser.setPhone(phone);

                    appUserDAO.insertOrUpdateUser(appUser);
                    setEditable(false);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()

                );
            }
        }).start();
    }

    private void showDeactivateDialog() {
        // Build a Material-styled dialog matching app colors
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        builder.setTitle("Deactivate Account");
        builder.setMessage("Are you sure you want to deactivate this account?\nPlease enter your NIC to confirm.");

        // Build an input layout for NIC
        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setHint("NIC");

        final TextInputEditText nicInput = new TextInputEditText(this);
        nicInput.setHint("Enter NIC");
        textInputLayout.addView(nicInput);

        int paddingPx = (int) (16 * getResources().getDisplayMetrics().density);
        textInputLayout.setPadding(paddingPx, paddingPx, paddingPx, 0);

        builder.setView(textInputLayout);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String enteredNic = nicInput.getText() != null ? nicInput.getText().toString().trim() : "";
            if (appUser != null && enteredNic.equals(appUser.getNic())) {
                deactivateAccount();
            } else {
                Toast.makeText(this, "NIC does not match!", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.show();
    }


    private void deactivateAccount() {
        new Thread(() -> {
            try {
                String response = ApiClient.patchRequest(
                        ProfileActivity.this,
                        Constants.DEACTIVATE_EV_OWNER_URL,
                        null
                );

                runOnUiThread(() -> {
                    Toast.makeText(ProfileActivity.this, response, Toast.LENGTH_LONG).show();

                    // Clear local DB
                    appUserDAO.clearUsers();

                    // Redirect to login
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(ProfileActivity.this, "Deactivation failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        }).start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_profile || itemId == R.id.nav_operator_profile) {
            // Already on profile; consume the event to keep highlight
            return true;
        } else if (itemId == R.id.nav_home || itemId == R.id.nav_operator_home) {
            Intent homeIntent = new Intent(this, DashboardActivity.class);
            if (itemId == R.id.nav_operator_home) {
                homeIntent = new Intent(this, OperatorHomeActivity.class);
            }
            startActivity(homeIntent);
            return true;
        } else if (itemId == R.id.nav_bookings || itemId == R.id.nav_operator_bookings) {
            Intent bookingsIntent = (itemId == R.id.nav_operator_bookings)
                    ? new Intent(this, OperatorBookingsActivity.class)
                    : new Intent(this, ReservationListActivity.class);
            startActivity(bookingsIntent);
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
