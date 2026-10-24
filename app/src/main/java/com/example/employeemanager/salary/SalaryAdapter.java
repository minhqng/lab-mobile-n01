package com.example.employeemanager.salary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.util.Constants;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Adapter hiển thị danh sách bảng lương cho RecyclerView trong {@link SalaryListActivity}.
 */
public class SalaryAdapter extends RecyclerView.Adapter<SalaryAdapter.SalaryViewHolder> {

    public interface OnSalaryClickListener {
        void onSalaryClick(Salary salary);
    }

    private final List<Salary> salaryList = new ArrayList<>();
    private Map<Long, Employee> employeeMap;
    private final OnSalaryClickListener listener;

    public SalaryAdapter(OnSalaryClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Salary> salaries, Map<Long, Employee> employees) {
        this.salaryList.clear();
        if (salaries != null) {
            this.salaryList.addAll(salaries);
        }
        this.employeeMap = employees;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SalaryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_salary, parent, false);
        return new SalaryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SalaryViewHolder holder, int position) {
        Salary salary = salaryList.get(position);
        Employee employee = (employeeMap != null) ? employeeMap.get(salary.employeeId) : null;
        holder.bind(salary, employee, listener);
    }

    @Override
    public int getItemCount() {
        return salaryList.size();
    }

    static class SalaryViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvEmployeeName;
        private final TextView tvStatus;
        private final TextView tvWorkHours;
        private final TextView tvTotalSalary;

        SalaryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEmployeeName = itemView.findViewById(R.id.tvEmployeeName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvWorkHours = itemView.findViewById(R.id.tvWorkHours);
            tvTotalSalary = itemView.findViewById(R.id.tvTotalSalary);
        }

        void bind(Salary salary, Employee employee, OnSalaryClickListener listener) {
            Context context = itemView.getContext();

            String employeeDisplay;
            if (employee != null) {
                employeeDisplay = context.getString(R.string.luong_format_employee_display, employee.code, employee.fullname);
            } else {
                employeeDisplay = context.getString(R.string.luong_fallback_employee_short, salary.employeeId);
            }
            tvEmployeeName.setText(employeeDisplay);

            String hoursText = context.getString(R.string.luong_format_work_hours, salary.totalHours, salary.overtimeHours);
            tvWorkHours.setText(hoursText);

            NumberFormat numberFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            String salaryText = context.getString(R.string.luong_format_currency,
                    numberFormat.format(salary.totalSalary), context.getString(R.string.luong_unit_currency));
            tvTotalSalary.setText(salaryText);

            if (Constants.STATUS_PAID.equals(salary.status)) {
                tvStatus.setText(R.string.luong_status_paid);
                tvStatus.setBackgroundResource(R.drawable.bg_status_paid);
                tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            } else {
                tvStatus.setText(R.string.luong_status_draft);
                tvStatus.setBackgroundResource(R.drawable.bg_status_draft);
                tvStatus.setTextColor(ContextCompat.getColor(context, android.R.color.holo_orange_dark));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onSalaryClick(salary);
                }
            });
        }
    }
}
