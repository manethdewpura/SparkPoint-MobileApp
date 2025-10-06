package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
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

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_operator_home);
        bottomNavigation.setOnItemSelectedListener(this);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.nav_operator_home) {
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
}
