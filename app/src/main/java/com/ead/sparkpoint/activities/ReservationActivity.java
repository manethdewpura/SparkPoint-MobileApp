package com.ead.sparkpoint.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReservationActivity extends AppCompatActivity {

    private Spinner spinnerStations, spinnerSlots;
    private EditText etDate, etNumSlots;
    private Button btnConfirm;

    private String bookingId = null;
    private String selectedStationId, selectedDate, selectedSlot;

    private static class SlotInfo {
        String displayName;
        String startTime;
        int availableSlots;

        SlotInfo(String displayName, String startTime, int availableSlots) {
            this.displayName = displayName;
            this.startTime = startTime;
            this.availableSlots = availableSlots;
        }
    }
    private Map<String, SlotInfo> slotMap = new HashMap<>();
    private List<String> slotsList = new ArrayList<>();

    // Station map for spinner prefill
    private Map<String, String> stationMap = new HashMap<>();
    private List<String> stationNames = new ArrayList<>();

    private static final String ACCESS_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJuYW1laWQiOiI2OGQ2ODhmNzM5MGVjYzY0YjBmMTlmNWIiLCJ1bmlxdWVfbmFtZSI6InNhbmRpdGhpIiwiZW1haWwiOiJzYW5kaXRoaW5ldGhzaWx1bmlAZ21haWwuY29tIiwicm9sZSI6IjMiLCJuYmYiOjE3NTkzOTM0NjksImV4cCI6MTc1OTQyOTQ2OSwiaWF0IjoxNzU5MzkzNDY5LCJpc3MiOiJTcGFya1BvaW50X1NlcnZlciIsImF1ZCI6IlNwYXJrUG9pbnRfQ2xpZW50In0.Lnf3q2ynJ1P5uVMW22fexfxWA5Pyv6PX3V96WoIcOv4";
    private boolean isUpdateMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservation);

        spinnerStations = findViewById(R.id.spinnerStations);
        spinnerSlots = findViewById(R.id.spinnerSlots);
        etDate = findViewById(R.id.etDate);
        etNumSlots = findViewById(R.id.etNumSlots);
        btnConfirm = findViewById(R.id.btnConfirm);

        // Check if update mode
        Intent intent = getIntent();
        if (intent != null && "update".equals(intent.getStringExtra("mode"))) {
            isUpdateMode = true;
            bookingId = intent.getStringExtra("bookingId");
            btnConfirm.setText("Update Booking");

            // Fetch booking details from server
            fetchBookingDetails(bookingId);
        }

        // set today's date if not update
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        if (selectedDate == null) {
            selectedDate = sdf.format(calendar.getTime());
        }
        etDate.setText(selectedDate);

        // open date picker
        etDate.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        selectedDate = year + "-" + String.format("%02d", (month+1)) + "-" + String.format("%02d", day);
                        etDate.setText(selectedDate);
                        if (selectedStationId != null) loadSlots(selectedStationId, selectedDate);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dpd.getDatePicker().setMinDate(System.currentTimeMillis());
            dpd.show();
        });

        // load stations
        loadStations();

        btnConfirm.setOnClickListener(v -> showSummaryDialog());
    }

    private void fetchBookingDetails(String bookingId) {
        new Thread(() -> {
            try {
                String url = Constants.UPDATE_BOOKINGS_URL.replace("{bookingid}", bookingId);
                String response = ApiClient.getRequest(url, ACCESS_TOKEN);
                JSONObject obj = new JSONObject(response);

                selectedStationId = obj.getJSONObject("station").getString("id");
                selectedDate = obj.getString("reservationTime").split("T")[0];
                selectedSlot = obj.getString("timeSlotDisplay");
                int slots = obj.getInt("slotsRequested");

                runOnUiThread(() -> {
                    etNumSlots.setText(String.valueOf(slots));
                    etDate.setText(selectedDate);

                    // Prefill spinner after stations are loaded
                    if (!stationNames.isEmpty()) prefillStationSpinner();

                    // Load slots for selected station and date
                    if (selectedStationId != null) loadSlots(selectedStationId, selectedDate);
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to fetch booking details", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void loadStations() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(Constants.GET_NEARBY_STATIONS_URL, ACCESS_TOKEN);
                JSONArray arr = new JSONArray(response);

                stationNames.clear();
                stationMap.clear();

                for (int i=0; i<arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    stationNames.add(name);
                    stationMap.put(name, id);
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, stationNames);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerStations.setAdapter(adapter);

                    spinnerStations.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            String stationName = stationNames.get(pos);
                            selectedStationId = stationMap.get(stationName);
                            loadSlots(selectedStationId, selectedDate);
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });

                    // Prefill spinner if in update mode
                    if (isUpdateMode && selectedStationId != null) prefillStationSpinner();
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void prefillStationSpinner() {
        for (int i = 0; i < stationNames.size(); i++) {
            String stationName = stationNames.get(i);
            if (selectedStationId.equals(stationMap.get(stationName))) {
                spinnerStations.setSelection(i);
                break;
            }
        }
    }

    private void loadSlots(String stationId, String date) {
        spinnerSlots.setEnabled(false);
        new Thread(() -> {
            try {
                String endpoint = "/bookings/availability/" + stationId + "/date/" + date;
                String response = ApiClient.getRequest(endpoint, ACCESS_TOKEN);

                JSONObject obj = new JSONObject(response);
                JSONArray arr = obj.getJSONArray("availabilityInfo");

                slotsList.clear();
                slotMap.clear();

                for (int i=0; i<arr.length(); i++) {
                    JSONObject slot = arr.getJSONObject(i);
                    String displayName = slot.getString("displayName");
                    String startTime = slot.getString("startTime");
                    int availableSlots = slot.getInt("availableSlots");
                    boolean available = slot.getBoolean("isAvailable");

                    if (available && availableSlots > 0) {
                        slotsList.add(displayName + " (" + availableSlots + " available)");
                    } else {
                        slotsList.add(displayName + " (Not available)");
                    }
                    slotMap.put(displayName, new SlotInfo(displayName, startTime, availableSlots));
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, slotsList);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerSlots.setAdapter(adapter);
                    spinnerSlots.setEnabled(!slotsList.isEmpty());

                    spinnerSlots.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                            String slotItem = slotsList.get(pos);
                            if (slotItem != null) selectedSlot = slotItem.split("\\(")[0].trim();
                            else selectedSlot = null;
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) { selectedSlot = null; }
                    });

                    // Prefill slot in update mode
                    if (isUpdateMode && selectedSlot != null) {
                        for (int i = 0; i < slotsList.size(); i++) {
                            if (slotsList.get(i).startsWith(selectedSlot)) {
                                spinnerSlots.setSelection(i);
                                break;
                            }
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showSummaryDialog() {
        String numSlotsStr = etNumSlots.getText().toString().trim();
        if (numSlotsStr.isEmpty()) {
            Toast.makeText(this, "Enter number of slots", Toast.LENGTH_SHORT).show();
            return;
        }

        int requestedSlots = Integer.parseInt(numSlotsStr);
        if (requestedSlots < 1 || requestedSlots > 5) {
            Toast.makeText(this, "Slots must be between 1 and 5", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedSlot == null || !slotMap.containsKey(selectedSlot)) {
            Toast.makeText(this, "Please select a valid slot", Toast.LENGTH_SHORT).show();
            return;
        }

        SlotInfo slotInfo = slotMap.get(selectedSlot);
        if (slotInfo == null || slotInfo.availableSlots < requestedSlots) {
            Toast.makeText(this, "Not enough slots available for this time", Toast.LENGTH_LONG).show();
            return;
        }

        String summary = "Booking Summary:\n\n" +
                "Station Id: " + selectedStationId + "\n" +
                "Date & Time: " + slotInfo.startTime + "\n" +
                "Number of Slots Booked: " + requestedSlots;

        new AlertDialog.Builder(this)
                .setTitle(isUpdateMode ? "Update Booking" : "Confirm Booking")
                .setMessage(summary)
                .setPositiveButton("Submit", (dialog, which) -> {
                    if (isUpdateMode) updateBooking(slotInfo.startTime, requestedSlots);
                    else submitBooking(slotInfo.startTime, requestedSlots);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void submitBooking(String slot, int Noslots) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("stationId", selectedStationId);
                body.put("reservationTime", slot );
                body.put("slotsRequested", Noslots);

                ApiClient.postRequest(Constants.CREATE_BOOKINGS_URL, body.toString(), ACCESS_TOKEN);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Booking Successful!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Booking Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void updateBooking(String slot, int Noslots) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("stationId", selectedStationId);
                body.put("reservationTime", slot);
                body.put("slotsRequested", Noslots);

                String url = Constants.UPDATE_BOOKINGS_URL.replace("{bookingid}", bookingId);
                ApiClient.patchRequest(url, body.toString(), ACCESS_TOKEN);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Booking Updated Successfully!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Update Failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}
