package com.ead.sparkpoint.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStructure;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.ead.sparkpoint.R;
import com.ead.sparkpoint.models.Reservation;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReservationAdapter extends RecyclerView.Adapter<ReservationAdapter.ViewHolder> {

    private List<Reservation> reservationList;
    private Context context;
    private OnReservationActionListener listener;

    public interface OnReservationActionListener {
        void onViewQR(Reservation reservation);
        void onUpdate(Reservation reservation);
        void onDelete(Reservation reservation);
    }

    public ReservationAdapter(List<Reservation> reservationList, Context context, OnReservationActionListener listener) {
        this.reservationList = reservationList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_reservation, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Reservation r = reservationList.get(position);
        holder.tvStationName.setText(r.getStationName());
        String dateTime = r.getReservationTime();   // e.g. "2025-10-03T00:00:00Z"
        OffsetDateTime odt = OffsetDateTime.parse(dateTime);
        String formattedDate = odt.toLocalDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        holder.tvDate.setText("Reservation Date: " + formattedDate);
        holder.tvTime.setText("Reservation Time: " + r.getReservationSlot());
        holder.tvStatus.setText(r.getStatus());

        //Apply status colours dynamically
        switch (r.getStatus()) {
            case "Pending":
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#40FF7600"))); // orange bg 25%
                holder.tvStatus.setTextColor(Color.parseColor("#FF7600")); // full orange text
                break;
            case "Confirmed":
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#400191FE"))); // light blue bg 25%
                holder.tvStatus.setTextColor(Color.parseColor("#0191FE")); // full light blue text
                break;
            case "In Progress":
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#40FFD700"))); // dark yellow bg 25%
                holder.tvStatus.setTextColor(Color.parseColor("#FFD700")); // full dark yellow text
                break;
            case "Completed":
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#404CAF50"))); // green bg 25%
                holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // full green text
                break;
            case "Cancelled":
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#40F44336"))); // red bg 25%
                holder.tvStatus.setTextColor(Color.parseColor("#F44336")); // full red text
                break;
            case "No Show":
                holder.tvStatus.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#409E9E9E"))); // gray bg 25%
                holder.tvStatus.setTextColor(Color.parseColor("#9E9E9E")); // full gray text
                break;
        }


        if ("Confirmed".equalsIgnoreCase(r.getStatus())) {
            holder.btnViewQR.setVisibility(View.VISIBLE);
            holder.btnViewQR.setOnClickListener(v -> listener.onViewQR(r));
        } else {
            holder.btnViewQR.setVisibility(View.GONE);
        }

        holder.btnUpdate.setOnClickListener(v -> listener.onUpdate(r));
        holder.btnDelete.setOnClickListener(v -> listener.onDelete(r));
    }

    @Override
    public int getItemCount() {
        return reservationList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStationName, tvDate, tvTime, tvStatus;
        Button btnViewQR, btnUpdate, btnDelete;

        public ViewHolder(View itemView) {
            super(itemView);
            tvStationName = itemView.findViewById(R.id.tvStationName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            btnViewQR = itemView.findViewById(R.id.btnViewQR);
            btnUpdate = itemView.findViewById(R.id.btnUpdate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
