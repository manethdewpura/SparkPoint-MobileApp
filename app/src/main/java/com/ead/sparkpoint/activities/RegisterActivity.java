package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;
import com.ead.sparkpoint.utils.LoadingDialog;

import org.json.JSONObject;

public class RegisterActivity extends AppCompatActivity {

    EditText etUsername, etEmail, etFirstName, etLastName, etPassword, etNic, etPhone;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etPassword = findViewById(R.id.etPassword);
        etNic = findViewById(R.id.etNic);
        etPhone = findViewById(R.id.etPhone);
        btnRegister = findViewById(R.id.btnRegister);

        btnRegister.setOnClickListener(v -> registerEVOwner());
    }

    private void registerEVOwner() {
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();
        String password = etPassword.getText().toString();
        String nic = etNic.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();

        // --- Client-Side Validation ---
        boolean isValid = true;
        // Clear previous errors
        etUsername.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etNic.setError(null);
        etPhone.setError(null);

        if (username.isEmpty()) {
            etUsername.setError("Username is required");
            isValid = false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError(Html.fromHtml("<font color='#ff7600'>Invalid email format</font>"));
            isValid = false;
        }

        if (password.isEmpty()) {
            etPassword.setError(Html.fromHtml("<font color='#ff7600'>Password is required</font>"));
            isValid = false;
        }

        if (nic.isEmpty()) {
            etNic.setError(Html.fromHtml("<font color='#ff7600'>NIC is required</font>"));
            isValid = false;
        }

        if (phone.length() > 10) {
            etPhone.setError(Html.fromHtml("<font color='#ff7600'>Phone number cannot exceed 10 digits</font>"));
            isValid = false;
        }

        if (!isValid) {
            return; // Stop if validation fails
        }
        // --- End of Validation ---

        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Creating account..."));
        new Thread(() -> {
            try {
                JSONObject req = new JSONObject();
                req.put("Username", username);
                req.put("Email", email);
                req.put("FirstName", firstName);
                req.put("LastName", lastName);
                req.put("Password", password);
                req.put("NIC", nic);
                req.put("Phone", phone);

                String response = ApiClient.postRequest(RegisterActivity.this, Constants.REGISTER_EV_OWNER_URL, req.toString());

                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(RegisterActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loading.hide();
                    Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
