package com.example.employeemanager.reports;

import com.example.employeemanager.R;
import com.example.employeemanager.data.dto.MonthlySummary;
import com.example.employeemanager.util.Constants;

/** Tổng kết công hoàn tất của chính nhân viên. */
public class MonthlySummaryActivity extends ReportActivity {
    @Override protected String requiredRole() { return Constants.ROLE_EMPLOYEE; }
    @Override protected int titleResource() { return R.string.bc_monthly_title; }
    @Override protected String reportText(int month, int year) {
        MonthlySummary summary = reports.getMyMonthlySummary(month, year);
        if (summary.completedDays == 0) return getString(R.string.bc_empty_work);
        return getString(R.string.bc_month_summary, summary.completedDays, hours(summary.getTotalHours()),
                hours(summary.getTotalHours() - summary.getOvertimeHours()), hours(summary.getOvertimeHours()));
    }
}
