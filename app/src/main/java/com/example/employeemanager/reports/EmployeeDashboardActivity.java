package com.example.employeemanager.reports;

import android.os.Bundle;
import android.view.View;
import com.example.employeemanager.R;
import com.example.employeemanager.attendance.AttendanceActivity;
import com.example.employeemanager.attendance.AttendanceService;
import com.example.employeemanager.attendance.AuthSessionProvider;
import com.example.employeemanager.attendance.WorkHistoryActivity;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;
import com.example.employeemanager.util.DateTimeUtils;

/** Trang cá nhân chỉ đọc hồ sơ và công của tài khoản đang đăng nhập. */
public class EmployeeDashboardActivity extends ReportActivity {
    @Override protected String requiredRole() { return Constants.ROLE_EMPLOYEE; }
    @Override protected int titleResource() { return R.string.bc_employee_title; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        dashboard();
        findViewById(R.id.bc_period).setVisibility(View.GONE);
        ((android.widget.Button) findViewById(R.id.bc_load)).setText(R.string.bc_refresh);
        navigate(R.id.bc_attendance, AttendanceActivity.class);
        navigate(R.id.bc_history, WorkHistoryActivity.class);
        navigate(R.id.bc_summary, MonthlySummaryActivity.class);
        navigate(R.id.bc_salary, MySalaryActivity.class);
    }
    @Override protected String reportText(int month, int year) {
        Employee employee = reports.getMyEmployee();
        WorkLog today = new AttendanceService(AppDB.getInstance(this), new AuthSessionProvider(sessions),
                DateTimeUtils.systemClock()).getTodayWorkLog();
        String status = getString(todayStatusResource(today));
        return getString(R.string.bc_employee_summary, employee.fullname, employee.code,
                employee.email, employee.phone, money(employee.hourlyRate), status);
    }

    private int todayStatusResource(WorkLog today) {
        if (today == null) {
            return R.string.bc_not_checked_in;
        }
        if (Constants.STATUS_OPEN.equals(today.status)) {
            return R.string.cong_status_open;
        }
        return R.string.cong_status_completed;
    }
}
