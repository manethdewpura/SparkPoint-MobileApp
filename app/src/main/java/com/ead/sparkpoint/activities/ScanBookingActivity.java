package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.ead.sparkpoint.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ScanBookingActivity extends AppCompatActivity {

    TextView tvScanHeading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_booking);

        tvScanHeading = findViewById(R.id.tvScanHeading);

        // Start scanner immediately
        new IntentIntegrator(this).initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String bookingId = result.getContents(); // QR contains bookingId
                Intent intent = new Intent(this, BookingDetailActivity.class);
                intent.putExtra("bookingId", bookingId);
                startActivity(intent);
                finish();
            } else {
                tvScanHeading.setText("Scan cancelled");
            }
        }
    }
}
