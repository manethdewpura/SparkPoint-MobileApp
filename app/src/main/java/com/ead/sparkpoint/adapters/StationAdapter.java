package com.ead.sparkpoint.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ead.sparkpoint.R;

import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.ViewHolder> {

    public static class StationItem {
        public final String id;
        public final String name;
        public final String address;
        public final String phone;

        public StationItem(String id, String name, String address, String phone) {
            this.id = id;
            this.name = name;
            this.address = address;
            this.phone = phone;
        }
    }

    public interface OnStationActionListener {
        void onAddBooking(StationItem station);
    }

    private final List<StationItem> stations;
    private final OnStationActionListener listener;

    public StationAdapter(List<StationItem> stations, OnStationActionListener listener) {
        this.stations = stations;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_station, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StationItem s = stations.get(position);
        holder.tvName.setText(s.name);
        holder.tvAddress.setText("Address: " + s.address);
        holder.tvPhone.setText("Contact Number: " + s.phone);
        holder.btnAddBooking.setOnClickListener(v -> listener.onAddBooking(s));
    }

    @Override
    public int getItemCount() {
        return stations.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvPhone;
        Button btnAddBooking;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvStationName);
            tvAddress = itemView.findViewById(R.id.tvStationAddress);
            tvPhone = itemView.findViewById(R.id.tvStationPhone);
            btnAddBooking = itemView.findViewById(R.id.btnAddBooking);
        }
    }
}



