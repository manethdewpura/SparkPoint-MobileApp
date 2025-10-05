package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;
import com.google.android.material.appbar.MaterialToolbar;

public class EVOwnerHomeActivity extends AppCompatActivity {

    TextView tvWelcomeOwner, tvOwnerEmail, tvOwnerNic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_evowner_home);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar); // ✅ This enables the menu

        tvWelcomeOwner = findViewById(R.id.tvWelcomeOwner);
        tvOwnerEmail = findViewById(R.id.tvOwnerEmail);
        tvOwnerNic = findViewById(R.id.tvOwnerNic);

        AppUserDAO dao = new AppUserDAO(this);
        AppUser user = dao.getUser();

        if (user != null) {
            tvWelcomeOwner.setText("Welcome " + user.getUsername());
            tvOwnerEmail.setText("Email: " + user.getEmail());
            tvOwnerNic.setText("NIC: " + user.getNic());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_evowner_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
