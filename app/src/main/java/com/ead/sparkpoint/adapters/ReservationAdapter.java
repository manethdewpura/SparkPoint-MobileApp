package com.ead.sparkpoint.adapters;

import android.content.Context;
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
        holder.tvDate.setText("Date: " + r.getReservationTime());
        holder.tvTime.setText("Reserved Slot: " + r.getReservationSlot());
        holder.tvStatus.setText("Status: " + r.getStatus());

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
