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

                if (res.has("message")) {
                    String msg = res.getString("message");
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_LONG).show()
                    );
                    return;
                }

                JSONObject userJson = res.getJSONObject("user");

                AppUser appUser = new AppUser(
                        userJson.getString("id"),
                        userJson.getString("username"),
                        userJson.getString("email"),
                        userJson.getInt("roleId"),
                        userJson.getString("roleName"),
                        // Ensure these fields are in your userJson from the login API
                        userJson.has("firstName") ? userJson.getString("firstName") : null,
                        userJson.has("lastName") ? userJson.getString("lastName") : null,
                        userJson.has("password") ? userJson.getString("password") : null,
                        userJson.has("nic") ? userJson.getString("nic") : null,
                        userJson.has("phone") ? userJson.getString("phone") : null,
                        res.getString("accessToken"),
                        res.getString("refreshToken")
                );

                appUserDAO.clearUsers();
                appUserDAO.insertOrUpdateUser(appUser);

                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT).show();

                    Integer roleId = appUser.getRoleId();

                    if (roleId != null) {
                        if (roleId == 3) {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        } else if (roleId == 2) {
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
