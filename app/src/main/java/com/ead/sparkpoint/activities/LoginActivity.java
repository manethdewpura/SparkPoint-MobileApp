package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.ead.sparkpoint.utils.LoadingDialog;

import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin;
    AppUserDAO appUserDAO;

    /**
     * * Called when the activity is first created. Initializes the UI for the login screen,
     * sets up listeners for the login button and sign-up text.
     * */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        appUserDAO = new AppUserDAO(this);

        btnLogin.setOnClickListener(v -> loginUser());

        TextView tvSignUp = findViewById(R.id.tvSignUp);

        tvSignUp.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

    }

    /**
     * Handles the user login process and retrieves credentials from the input fields,
     * sends a login request to the API, and if success, saves user data locally and
     * navigates to the appropriate home screen based on the user's role.
     */
    private void loginUser() {
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Signing in..."));
        new Thread(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("Username", username);
                req.put("Password", password);

                String response = ApiClient.postRequest(LoginActivity.this, Constants.LOGIN_URL, req.toString());
                JSONObject res = new JSONObject(response);

                // Handle cases where the API returns an error message.
                if (res.has("message")) {
                    String msg = res.getString("message");
                    runOnUiThread(() -> {
                        loading.hide();
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                JSONObject userJson = res.getJSONObject("user");

                // Create a new AppUser object from the API response.
                AppUser appUser = new AppUser(
                        userJson.getString("id"),
                        userJson.getString("username"),
                        userJson.getString("email"),
                        userJson.getInt("roleId"),
                        userJson.getString("roleName"),
                        userJson.has("firstName") ? userJson.getString("firstName") : null,
                        userJson.has("lastName") ? userJson.getString("lastName") : null,
                        userJson.has("password") ? userJson.getString("password") : null,
                        userJson.has("nic") ? userJson.getString("nic") : null,
                        userJson.has("phone") ? userJson.getString("phone") : null,
                        res.getString("accessToken"),
                        res.getString("refreshToken")
                );

                // Clear any existing user data and save the new user to the local database.
                appUserDAO.clearUsers();
                appUserDAO.insertOrUpdateUser(appUser);

                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                    // Redirect user based on their role ID.

                    Integer roleId = appUser.getRoleId();
                    if (roleId != null) {//EV Owner
                        if (roleId == 3) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        } else if (roleId == 2) {//Station Operator
                            startActivity(new Intent(LoginActivity.this, OperatorHomeActivity.class));
                        } else {
                            Toast.makeText(LoginActivity.this, "Unknown user role ID: " + roleId, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(LoginActivity.this, "User role ID is missing.", Toast.LENGTH_LONG).show();
                    }
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(LoginActivity.this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }

        }).start();
    }
}
