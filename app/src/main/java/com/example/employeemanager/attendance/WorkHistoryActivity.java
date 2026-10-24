package com.example.employeemanager.attendance;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.DateTimeUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class WorkHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView textState;
    private TextView textSummary;
    private Spinner spinnerMonth;
    private Spinner spinnerYear;
    private WorkLogAdapter adapter;
    private AttendanceService attendanceService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_work_history);

        recyclerView = findViewById(R.id.recyclerWorkHistory);
        textState = findViewById(R.id.textHistoryState);
        textSummary = findViewById(R.id.textHistorySummary);
        spinnerMonth = findViewById(R.id.spinnerMonth);
        spinnerYear = findViewById(R.id.spinnerYear);
        Button buttonLoad = findViewById(R.id.buttonLoadHistory);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkLogAdapter();
        recyclerView.setAdapter(adapter);

        setupSpinners();

        SharedPreferences preferences = getSharedPreferences(
                AttendanceSessionContract.PREFERENCES_NAME, MODE_PRIVATE);
        AppDB database = AppDB.getInstance(getApplicationContext());
        attendanceService = new AttendanceService(
                database.workLogDao(),
                database.salaryDao(),
                new SharedPreferenceSessionProvider(
                        preferences, database.accountDao(), database.employeeDao()),
                DateTimeUtils.systemClock());

        buttonLoad.setOnClickListener(v -> loadHistory());
        loadHistory();
    }

    private void setupSpinners() {
        List<String> months = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            months.add(String.valueOf(month));
        }
        spinnerMonth.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, months));

        int currentYear = DateTimeUtils.today(DateTimeUtils.systemClock()).getYear();
        List<String> years = new ArrayList<>();
        for (int year = currentYear - 2; year <= currentYear + 1; year++) {
            years.add(String.valueOf(year));
        }
        spinnerYear.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, years));
        spinnerMonth.setSelection(LocalDate.now().getMonthValue() - 1);
        spinnerYear.setSelection(2);
    }

    private void loadHistory() {
        int month;
        int year;
        try {
            month = Integer.parseInt(String.valueOf(spinnerMonth.getSelectedItem()));
            year = Integer.parseInt(String.valueOf(spinnerYear.getSelectedItem()));
        } catch (Exception exception) {
            Toast.makeText(this, R.string.cong_error_invalid_period, Toast.LENGTH_SHORT).show();
            return;
        }
        if (month < 1 || month > 12 || year <= 0) {
            Toast.makeText(this, R.string.cong_error_invalid_period, Toast.LENGTH_SHORT).show();
            return;
        }

        textState.setText(R.string.cong_loading);
        adapter.setItems(new ArrayList<>());

        AppExecutors.databaseExecutor().execute(() -> {
            List<WorkLog> logs;
            String error = null;
            try {
                logs = attendanceService.getAttendanceHistory(month, year);
            } catch (Exception exception) {
                logs = new ArrayList<>();
                error = exception.getMessage();
            }
            final List<WorkLog> result = logs;
            final String finalError = error;
            final long totalMinutes = AttendanceSummaryCalculator.completedMinutes(result);
            final long overtime = AttendanceSummaryCalculator.overtimeMinutes(result);
            AppExecutors.runOnMainThread(() -> {
                if (finalError != null) {
                    textState.setText(finalError);
                    textSummary.setText(getString(R.string.cong_history_summary_empty));
                    adapter.setItems(new ArrayList<>());
                    return;
                }
                adapter.setItems(result);
                if (result.isEmpty()) {
                    textState.setText(R.string.cong_history_summary_empty);
                    textSummary.setText(getString(R.string.cong_history_summary_empty));
                } else {
                    textState.setText(getString(R.string.cong_loaded_count, result.size()));
                    textSummary.setText(getString(R.string.cong_history_summary,
                            DateTimeUtils.formatHours(
                                    WorkHourCalculator.minutesToHours(totalMinutes)),
                            DateTimeUtils.formatHours(
                                    WorkHourCalculator.minutesToHours(overtime))));
                }
            });
        });
    }
}
