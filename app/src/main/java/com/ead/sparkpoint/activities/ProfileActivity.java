package com.ead.sparkpoint.activities;

import android.os.Bundle;
import android.text.Html;
import android.text.InputType;
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
import com.ead.sparkpoint.utils.LoadingDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


public class ProfileActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {

    EditText etUsername, etEmail, etFirstName, etLastName, etPassword, etNic, etPhone;
    Button btnEdit, btnSave;
    AppUserDAO appUserDAO;
    AppUser appUser;

    /**
     * Initializes the profile activity. It populates user data, sets up UI components,
     * and adjusts the view based on the user's role (EV Owner vs. Station Operator).
     * @param savedInstanceState State data for activity re-creation.
     */
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

        // Dynamically set the bottom navigation menu based on user role.
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

        // Populate fields with user data.
        if (appUser != null) {
            etUsername.setText(appUser.getUsername());
            etEmail.setText(appUser.getEmail());
            etFirstName.setText(appUser.getFirstName());
            etLastName.setText(appUser.getLastName());
            etPassword.setText(appUser.getPassword());
            etNic.setText(appUser.getNic());
            etPhone.setText(appUser.getPhone());
        }

        // Customize UI for Station Operators (hide sensitive fields and action buttons)
        boolean isOperator = roleUser != null && Integer.valueOf(2).equals(roleUser.getRoleId());
        setEditable(false);// fields are read-only until user taps Edit
        if (isOperator) {
            // disable inputs
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
        });

    }

    /**
     * Toggles the editable state of the profile input fields.
     * @param enabled True to make fields editable, false to make them read-only.
     */
    private void setEditable(boolean enabled) {
        etUsername.setEnabled(enabled);
        etEmail.setEnabled(enabled);
        etFirstName.setEnabled(enabled);
        etLastName.setEnabled(enabled);
        etPassword.setEnabled(enabled);
        // NIC should not be editable as it is the primary key
        etNic.setEnabled(false);
        etPhone.setEnabled(enabled);

        btnSave.setEnabled(enabled);
    }

    /**
     * Gathers data from input fields, validates it, and sends an API request to update the user's profile.
     * On success, it also updates the local database.
     */
    private void updateProfile() {
        String username = etUsername.getText().toString();
        String email = etEmail.getText().toString().trim();
        String firstName = etFirstName.getText().toString();
        String lastName = etLastName.getText().toString();
        String password = etPassword.getText().toString();
        String nic = etNic.getText().toString();
        String phone = etPhone.getText().toString().trim();

        //Perform client-side validation.
        boolean isValid = true;
        etEmail.setError(null);
        etPhone.setError(null);

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(Html.fromHtml("<font color='#ff7600'>Invalid email format</font>"));
            isValid = false;
        }

        if (phone.length() > 10) {
            etPhone.setError(Html.fromHtml("<font color='#ff7600'>Phone number cannot exceed 10 digits</font>"));
            isValid = false;
        }

        if (!isValid) {
            return; // Stop if validation fails
        }

        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Saving changes..."));
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
                    loading.hide();
                    Toast.makeText(ProfileActivity.this, response, Toast.LENGTH_SHORT).show();

                    // update local DB
                    appUser.setUsername(username);
                    appUser.setEmail(email);
                    appUser.setFirstName(firstName);
                    appUser.setLastName(lastName);
                    appUser.setPassword(password);
                    appUser.setNic(nic);
                    appUser.setPhone(phone);

                    // Revert UI to view mode.
                    appUserDAO.insertOrUpdateUser(appUser);
                    setEditable(false);
                    btnEdit.setVisibility(android.view.View.VISIBLE);
                    btnSave.setVisibility(android.view.View.GONE);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(ProfileActivity.this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Displays a confirmation dialog for account deactivation. The user must enter their NIC to confirm.
     */
    private void showDeactivateDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        builder.setTitle("Deactivate Account");
        builder.setMessage("Are you sure you want to deactivate this account?\nPlease enter your NIC to confirm.");

        // Create TextInputLayout
        TextInputLayout textInputLayout = new TextInputLayout(this);
        textInputLayout.setHint("Enter NIC");

        // Create TextInputEditText correctly inside the layout
        TextInputEditText nicInput = new TextInputEditText(textInputLayout.getContext());
        nicInput.setInputType(InputType.TYPE_CLASS_TEXT); // optional
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

    /**
     * Sends an API request to deactivate the user's account. On success, clears local data and redirects to login.
     */
    private void deactivateAccount() {
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Deactivating account..."));
        new Thread(() -> {
            try {
                String response = ApiClient.patchRequest(
                        ProfileActivity.this,
                        Constants.DEACTIVATE_EV_OWNER_URL,
                        null
                );

                runOnUiThread(() -> {
                    loading.hide();
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
                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(ProfileActivity.this, "Deactivation failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    /**
     * Handles item selections from the bottom navigation bar, navigating to the correct screen.
     * @param item The menu item that was selected.
     * @return boolean True to display the item as the selected item.
     */
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.nav_profile || itemId == R.id.nav_operator_profile) {
            // Already on profile screen and consume the event to keep highlight
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
     * Sets up the top-right menu button to show a popup menu for logging out.
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
                        // Handle logout
                        new TokenManager(this).logoutUser();
                        return true;
                    }
                    return false;
                });

                popup.show();
            });
        }
    }
}