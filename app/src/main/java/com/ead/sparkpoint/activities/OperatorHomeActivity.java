package com.ead.sparkpoint.activities;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.database.AppUserDAO;
import com.ead.sparkpoint.models.AppUser;

public class OperatorHomeActivity extends AppCompatActivity {

    TextView tvWelcomeOperator, tvOperatorEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operator_home);

        tvWelcomeOperator = findViewById(R.id.tvWelcomeOperator);
        tvOperatorEmail = findViewById(R.id.tvOperatorEmail);

        AppUserDAO dao = new AppUserDAO(this);
        AppUser user = dao.getUser();

        if (user != null) {
            tvWelcomeOperator.setText("Welcome Operator " + user.getUsername());
            tvOperatorEmail.setText("Email: " + user.getEmail());
        }
    }
}
