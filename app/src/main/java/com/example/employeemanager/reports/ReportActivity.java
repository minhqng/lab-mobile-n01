package com.example.employeemanager.reports;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import com.example.employeemanager.R;
import com.example.employeemanager.auth.ProtectedActivity;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.util.DateTimeUtils;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Bộ lọc kỳ và trạng thái tải dùng chung cho các báo cáo ngắn. */
public abstract class ReportActivity extends ProtectedActivity {
    protected ReportService reports;
    protected TextView content;
    private Spinner month, year;
    private int currentYear;
    private boolean loading;
    protected abstract int titleResource();
    protected abstract String reportText(int month, int year);
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_report);
        reports = new ReportService(AppDB.getInstance(this), sessions);
        content = findViewById(R.id.bc_content);
        ((TextView) findViewById(R.id.bc_title)).setText(titleResource());
        month = findViewById(R.id.bc_month);
        year = findViewById(R.id.bc_year);
        LocalDate today = DateTimeUtils.today(DateTimeUtils.systemClock());
        currentYear = today.getYear();
        List<String> months = new ArrayList<>(), years = new ArrayList<>();
        for (int value = 1; value <= 12; value++) months.add(getString(R.string.luong_format_month, value));
        for (int value = currentYear - 2; value <= currentYear + 2; value++) years.add(String.valueOf(value));
        month.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, months));
        year.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, years));
        month.setSelection(state == null ? today.getMonthValue() - 1 : state.getInt("month"));
        year.setSelection(state == null ? 2 : state.getInt("year", 2));
        findViewById(R.id.bc_load).setOnClickListener(view -> refresh());
        findViewById(R.id.bc_back).setOnClickListener(view -> finish());
        findViewById(R.id.bc_logout).setOnClickListener(view -> logout());
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("month", month.getSelectedItemPosition());
        state.putInt("year", year.getSelectedItemPosition());
        super.onSaveInstanceState(state);
    }
    @Override protected void onSessionReady() { loading = false; refresh(); }
    protected void navigate(int buttonId, Class<?> target) {
        Button button = findViewById(buttonId);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(view -> startActivity(new Intent(this, target)));
    }
    protected void dashboard() {
        findViewById(R.id.bc_logout).setVisibility(View.VISIBLE);
        findViewById(R.id.bc_back).setVisibility(View.GONE);
    }
    protected String hours(double value) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN"));
        format.setMaximumFractionDigits(2);
        return getString(R.string.bc_hours, format.format(value));
    }
    protected String money(long value) {
        return getString(R.string.bc_money, NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value));
    }
    private void operating(boolean value) {
        loading = value;
        findViewById(R.id.bc_load).setEnabled(!value);
        month.setEnabled(!value);
        year.setEnabled(!value);
        findViewById(R.id.bc_progress).setVisibility(value ? View.VISIBLE : View.GONE);
    }
    private void refresh() {
        if (loading) return;
        int selectedMonth = month.getSelectedItemPosition() + 1;
        int selectedYear = currentYear - 2 + year.getSelectedItemPosition();
        operating(true);
        content.setText("");
        ((TextView) findViewById(R.id.bc_status)).setText(R.string.cong_loading);
        runTask(() -> reportText(selectedMonth, selectedYear), text -> {
            operating(false);
            content.setText(text);
            ((TextView) findViewById(R.id.bc_status)).setText(getString(R.string.bc_loaded, selectedMonth, selectedYear));
        }, error -> {
            operating(false);
            content.setText(errorText(error));
            ((TextView) findViewById(R.id.bc_status)).setText(R.string.bc_data_error);
        });
    }
}
