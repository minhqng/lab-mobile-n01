package com.example.employeemanager.attendance;

import com.example.employeemanager.auth.SessionManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.DateTimeUtils;

public class AttendanceActivity extends com.example.employeemanager.auth.ProtectedActivity {
    @Override protected String requiredRole() { return com.example.employeemanager.util.Constants.ROLE_EMPLOYEE; }

    private TextView textDate;
    private TextView textTime;
    private TextView textCheckIn;
    private TextView textCheckOut;
    private TextView textStatus;
    private Button buttonCheckIn;
    private Button buttonCheckOut;

    private AttendanceService attendanceService;
    private final android.os.Handler timeHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable updateClock = new Runnable() {
        @Override public void run() {
            textDate.setText(getString(R.string.cong_date_value,
                    DateTimeUtils.formatDate(DateTimeUtils.today(attendanceService.getClock()))));
            textTime.setText(getString(R.string.cong_time_value,
                    DateTimeUtils.formatTime(DateTimeUtils.nowTime(attendanceService.getClock()))));
            timeHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance);

        textDate = findViewById(R.id.textAttendanceDate);
        textTime = findViewById(R.id.textAttendanceTime);
        textCheckIn = findViewById(R.id.textCheckInTime);
        textCheckOut = findViewById(R.id.textCheckOutTime);
        textStatus = findViewById(R.id.textAttendanceStatus);
        buttonCheckIn = findViewById(R.id.buttonCheckIn);
        buttonCheckOut = findViewById(R.id.buttonCheckOut);
        Button buttonHistory = findViewById(R.id.buttonWorkHistory);

        AppDB database = AppDB.getInstance(getApplicationContext());
        attendanceService = new AttendanceService(database, new AuthSessionProvider(new SessionManager(this, database)),
                DateTimeUtils.systemClock());

        buttonCheckIn.setOnClickListener(v -> doCheckIn());
        buttonCheckOut.setOnClickListener(v -> doCheckOut());
        buttonHistory.setOnClickListener(v ->
                startActivity(new android.content.Intent(this, WorkHistoryActivity.class)));

    }

    @Override
    protected void onSessionReady() {
        refresh();
        timeHandler.removeCallbacks(updateClock);
        timeHandler.post(updateClock);
    }

    @Override protected void onPause() {
        timeHandler.removeCallbacks(updateClock);
        super.onPause();
    }

    private void refresh() {
        textDate.setText(getString(R.string.cong_date_value,
                DateTimeUtils.formatDate(DateTimeUtils.today(attendanceService.getClock()))));
        textTime.setText(getString(R.string.cong_time_value,
                DateTimeUtils.formatTime(DateTimeUtils.nowTime(attendanceService.getClock()))));

        buttonCheckIn.setEnabled(false);
        buttonCheckOut.setEnabled(false);
        textCheckIn.setText(getString(R.string.cong_checkin_none));
        textCheckOut.setText(getString(R.string.cong_checkout_none));
        textStatus.setText(getString(R.string.cong_status_open));

        AppExecutors.databaseExecutor().execute(() -> {
            WorkLogResult result = new WorkLogResult();
            try {
                result.log = attendanceService.getTodayWorkLog();
            } catch (Exception exception) {
                result.error = exception instanceof AttendanceException ? exception.getMessage() : errorText(exception);
            }
            AppExecutors.runOnMainThread(() -> applyRefresh(result));
        });
    }

    private void applyRefresh(WorkLogResult result) {
        if (isFinishing() || isDestroyed()) return;
        if (result.error != null) {
            textCheckIn.setText(result.error);
            textCheckOut.setText("");
            textStatus.setText("");
            buttonCheckIn.setEnabled(false);
            buttonCheckOut.setEnabled(false);
            Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show();
            return;
        }
        WorkLog log = result.log;
        if (log == null) {
            textCheckIn.setText(getString(R.string.cong_checkin_none));
            textCheckOut.setText(getString(R.string.cong_checkout_none));
            textStatus.setText(R.string.bc_not_checked_in);
            buttonCheckIn.setEnabled(true);
            buttonCheckOut.setEnabled(false);
            return;
        }
        textCheckIn.setText(getString(R.string.cong_checkin_value,
                DateTimeUtils.formatTime(log.checkInTime)));
        if (log.checkOutTime == null) {
            textCheckOut.setText(getString(R.string.cong_checkout_none));
            textStatus.setText(getString(R.string.cong_status_open));
            buttonCheckIn.setEnabled(false);
            buttonCheckOut.setEnabled(true);
        } else {
            textCheckOut.setText(getString(R.string.cong_checkout_value,
                    DateTimeUtils.formatTime(log.checkOutTime)));
            textStatus.setText(getString(R.string.cong_status_completed));
            buttonCheckIn.setEnabled(false);
            buttonCheckOut.setEnabled(false);
        }
    }

    private void doCheckIn() {
        buttonCheckIn.setEnabled(false);
        AppExecutors.databaseExecutor().execute(() -> {
            String message;
            try {
                WorkLog log = attendanceService.checkIn();
                message = getString(R.string.cong_checkin_success,
                        DateTimeUtils.formatTime(log.checkInTime));
            } catch (Exception exception) {
                message = exception instanceof AttendanceException ? exception.getMessage() : errorText(exception);
            }
            final String finalMessage = message;
            AppExecutors.runOnMainThread(() -> {
                Toast.makeText(this, finalMessage, Toast.LENGTH_SHORT).show();
                refresh();
            });
        });
    }

    private void doCheckOut() {
        buttonCheckOut.setEnabled(false);
        AppExecutors.databaseExecutor().execute(() -> {
            String message;
            try {
                WorkLog log = attendanceService.checkOut();
                message = getString(R.string.cong_checkout_success) + " - "
                        + getString(R.string.cong_hours_today,
                        DateTimeUtils.formatHours(log.totalHours));
            } catch (Exception exception) {
                message = exception instanceof AttendanceException ? exception.getMessage() : errorText(exception);
            }
            final String finalMessage = message;
            AppExecutors.runOnMainThread(() -> {
                Toast.makeText(this, finalMessage, Toast.LENGTH_SHORT).show();
                refresh();
            });
        });
    }

    private static class WorkLogResult {
        WorkLog log;
        String error;
    }
}
