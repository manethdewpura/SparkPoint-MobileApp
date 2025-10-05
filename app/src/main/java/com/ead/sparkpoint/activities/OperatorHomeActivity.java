package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;

public class OperatorHomeActivity extends AppCompatActivity {

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

        btnScanQR.setOnClickListener(v -> {
            startActivity(new Intent(OperatorHomeActivity.this, ScanBookingActivity.class));
        });
    }
}
