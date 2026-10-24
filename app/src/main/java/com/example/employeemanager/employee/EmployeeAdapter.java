package com.example.employeemanager.employee;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.Employee;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class EmployeeAdapter extends RecyclerView.Adapter<EmployeeAdapter.Holder> {
    private final List<Employee> employees = new ArrayList<>();
    private final Consumer<Employee> onClick;

    public EmployeeAdapter(Consumer<Employee> onClick) { this.onClick = onClick; }

    public void submit(List<Employee> data) {
        int previous = employees.size();
        employees.clear();
        notifyItemRangeRemoved(0, previous);
        employees.addAll(data);
        notifyItemRangeInserted(0, employees.size());
    }

    public static String formatRate(View view, long rate) {
        return view.getContext().getString(R.string.nv_gia_tri_don_gia,
                NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(rate));
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Employee employee = employees.get(position);
        holder.code.setText(employee.code);
        holder.name.setText(employee.fullname);
        holder.email.setText(employee.email);
        holder.phone.setText(employee.phone);
        holder.rate.setText(formatRate(holder.itemView, employee.hourlyRate));
        holder.itemView.setOnClickListener(view -> onClick.accept(employee));
    }

    @Override public int getItemCount() { return employees.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView code, name, email, phone, rate;
        Holder(View view) {
            super(view);
            code = view.findViewById(R.id.nv_code);
            name = view.findViewById(R.id.nv_fullname);
            email = view.findViewById(R.id.nv_email);
            phone = view.findViewById(R.id.nv_phone);
            rate = view.findViewById(R.id.nv_hourly_rate);
        }
    }
}
