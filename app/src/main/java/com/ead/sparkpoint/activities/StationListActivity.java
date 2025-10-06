package com.ead.sparkpoint.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ead.sparkpoint.R;
import com.ead.sparkpoint.adapters.StationAdapter;
import com.ead.sparkpoint.utils.ApiClient;
import com.ead.sparkpoint.utils.Constants;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class StationListActivity extends AppCompatActivity implements StationAdapter.OnStationActionListener {

    private RecyclerView recyclerStations;
    private StationAdapter adapter;
    private final List<StationAdapter.StationItem> stations = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_station_list);

        recyclerStations = findViewById(R.id.recyclerStations);
        recyclerStations.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StationAdapter(stations, this);
        recyclerStations.setAdapter(adapter);

        loadStations();
    }

    private void loadStations() {
        new Thread(() -> {
            try {
                String response = ApiClient.getRequest(StationListActivity.this, Constants.GET_NEARBY_STATIONS_URL);
                JSONArray arr = new JSONArray(response);
                stations.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String id = obj.getString("id");
                    String name = obj.getString("name");
                    String address = obj.optString("address", "");
                    String phone = obj.optString("contactPhone", "");
                    stations.add(new StationAdapter.StationItem(id, name, address, phone));
                }
                runOnUiThread(() -> adapter.notifyDataSetChanged());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Failed to load stations", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onAddBooking(StationAdapter.StationItem station) {
        Intent intent = new Intent(this, ReservationActivity.class);
        intent.putExtra("lockStation", true);
        intent.putExtra("stationId", station.id);
        intent.putExtra("stationName", station.name);
        startActivity(intent);
    }
}



