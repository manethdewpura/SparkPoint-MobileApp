package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.utils.LoadingDialog;
import com.ead.sparkpoint.utils.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;

public class QRCodeActivity extends AppCompatActivity implements NavigationBarView.OnItemSelectedListener {
    private ImageView qrImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Initialize QR screen, wire navigation/menu, and render QR for booking
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        qrImage = findViewById(R.id.qrImage);

        // Setup bottom navigation
        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        bottomNavigation.setSelectedItemId(R.id.nav_bookings);
        bottomNavigation.setOnItemSelectedListener(this);
        
        // Setup menu button
        setupMenuButton();

        String bookingId = getIntent().getStringExtra("bookingId");

        String qrData = bookingId;

        generateQRCode(qrData);
    }

    private void generateQRCode(String data) {
        // Generate a bitmap QR code from provided data and display it
        QRCodeWriter writer = new QRCodeWriter();
        try {
            int size = 512;
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size);
            Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);

            for (int x = 0; x < size; x++) {
                for (int y = 0; y < size; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            qrImage.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle bottom navigation item taps and navigate between screens
        int itemId = item.getItemId();
        
        if (itemId == R.id.nav_home) {
            Intent homeIntent = new Intent(this, DashboardActivity.class);
            startActivity(homeIntent);
            return true;
        } else if (itemId == R.id.nav_bookings) {
            // Already on bookings screen
            return true;
        } else if (itemId == R.id.nav_profile) {
            Intent profileIntent = new Intent(this, ProfileActivity.class);
            startActivity(profileIntent);
            return true;
        }
        
        return false;
    }

    private void setupMenuButton() {
        // Wire up the top app bar menu and handle logout action
        ImageButton menuButton = findViewById(R.id.menuButton);
        if (menuButton != null) {
            menuButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(this, menuButton);
                popup.getMenuInflater().inflate(R.menu.top_app_bar_menu, popup.getMenu());
                
                // Ensure icons are shown in the popup
                try {
                    java.lang.reflect.Field mFieldPopup = PopupMenu.class.getDeclaredField("mPopup");
                    mFieldPopup.setAccessible(true);
                    Object mPopup = mFieldPopup.get(popup);
                    mPopup.getClass().getDeclaredMethod("setForceShowIcon", boolean.class)
                            .invoke(mPopup, true);
                } catch (Exception ignored) { }

                popup.setOnMenuItemClickListener(item -> {
                    int itemId = item.getItemId();
                    if (itemId == R.id.menu_logout) {
                        logoutUser();
                        return true;
                    }
                    return false;
                });
                
                popup.show();
            });
        }
    }

    private void logoutUser() {
        // Perform logout in background and finish the activity when done
        LoadingDialog loading = new LoadingDialog(this);
        runOnUiThread(() -> loading.show("Signing out..."));
        new Thread(() -> {
            try {
                TokenManager tokenManager = new TokenManager(this);
                tokenManager.logoutUser();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loading.hide();
                    finish();
                });
                return;
            }
            runOnUiThread(loading::hide);
        }).start();
    }
}