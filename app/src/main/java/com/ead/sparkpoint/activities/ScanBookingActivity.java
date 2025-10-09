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

    /**
     * Called when the activity is first created. This activity's main purpose is to
     * immediately initiate the QR code scanning process.
     * @param savedInstanceState If the activity is being re-initialized, this Bundle contains
     * the most recent data, otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_booking);

        tvScanHeading = findViewById(R.id.tvScanHeading);

        // Start scanner immediately
        new IntentIntegrator(this).initiateScan();
    }

    /**
     * Callback for the result from launching the QR scanner. This method handles the
     * outcome of the scan.
     * @param requestCode The integer request code originally supplied to startActivityForResult().
     * @param resultCode The integer result code returned by the child activity.
     * @param data An Intent, which can return result data to the caller.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Parse the activity result using the ZXing library.
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            // Check if the scanner found a QR code.
            if (result.getContents() != null) {
                String bookingId = result.getContents(); // QR contains bookingId
                // Create an intent to open the BookingDetailActivity.
                Intent intent = new Intent(this, BookingDetailActivity.class);
                intent.putExtra("bookingId", bookingId);
                startActivity(intent);
                finish();
            } else {
                // Handle the case where the user cancels the scan.
                tvScanHeading.setText("Scan cancelled");
            }
        }
    }
}
