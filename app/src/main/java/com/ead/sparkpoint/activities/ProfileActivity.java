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


public class ProfileActivity extends AppCompatActivity {

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

        setEditable(false);

        btnEdit.setOnClickListener(v -> setEditable(true));

        btnSave.setOnClickListener(v -> updateProfile());
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
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Deactivate Account");
        builder.setMessage("Are you sure you want to deactivate this account? Please enter your NIC to confirm.");

        final EditText input = new EditText(this);
        input.setHint("Enter NIC");
        builder.setView(input);

        builder.setPositiveButton("Confirm", (dialog, which) -> {
            String enteredNic = input.getText().toString().trim();
            if (enteredNic.equals(appUser.getNic())) {
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


}
