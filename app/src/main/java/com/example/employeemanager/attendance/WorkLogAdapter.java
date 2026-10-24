package com.example.employeemanager.attendance;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;
import com.example.employeemanager.util.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Bộ điều hợp RecyclerView hiển thị lịch sử chấm công.
 */
public class WorkLogAdapter extends RecyclerView.Adapter<WorkLogAdapter.ViewHolder> {

    private final List<WorkLog> items = new ArrayList<>();

    public void setItems(List<WorkLog> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_work_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkLog log = items.get(position);
        holder.textWorkDate.setText(DateTimeUtils.formatDate(log.workDate));
        String checkIn = DateTimeUtils.formatTime(log.checkInTime);
        String checkOut = log.checkOutTime == null
                ? holder.itemView.getContext().getString(R.string.cong_checkout_missing)
                : DateTimeUtils.formatTime(log.checkOutTime);
        holder.textWorkTimes.setText(holder.itemView.getContext().getString(
                R.string.cong_work_times, checkIn, checkOut));
        long minutes = Math.max(0, Math.round(log.totalHours * 60.0));
        long otMinutes = WorkHourCalculator.overtimeMinutes(minutes);
        holder.textWorkHours.setText(holder.itemView.getContext().getString(
                R.string.cong_work_hours,
                DateTimeUtils.formatHours(log.totalHours),
                DateTimeUtils.formatHours(WorkHourCalculator.minutesToHours(otMinutes))));
        holder.textWorkStatus.setText(statusLabel(log.status, holder.itemView));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String statusLabel(String status, View view) {
        if (Constants.STATUS_OPEN.equals(status)) {
            return view.getContext().getString(R.string.cong_status_working);
        }
        if (Constants.STATUS_COMPLETED.equals(status)) {
            return view.getContext().getString(R.string.cong_status_completed_label);
        }
        return view.getContext().getString(R.string.cong_status_unknown);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView textWorkDate;
        final TextView textWorkTimes;
        final TextView textWorkHours;
        final TextView textWorkStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            textWorkDate = itemView.findViewById(R.id.textWorkDate);
            textWorkTimes = itemView.findViewById(R.id.textWorkTimes);
            textWorkHours = itemView.findViewById(R.id.textWorkHours);
            textWorkStatus = itemView.findViewById(R.id.textWorkStatus);
        }
    }
}
