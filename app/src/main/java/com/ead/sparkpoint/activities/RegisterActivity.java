package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;

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
                req.put("Password", password);
                req.put("NIC", nic);
                req.put("Phone", phone);

                String response = ApiClient.postRequest(Constants.REGISTER_EV_OWNER_URL, req.toString(), null);

                runOnUiThread(() -> {
                    Toast.makeText(RegisterActivity.this, "Registered Successfully", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(RegisterActivity.this, "Registration failed", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}
