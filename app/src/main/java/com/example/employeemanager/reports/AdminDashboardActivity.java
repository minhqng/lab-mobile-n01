package com.example.employeemanager.reports;

import android.os.Bundle;
import com.example.employeemanager.R;
import com.example.employeemanager.data.dto.CompanySummary;
import com.example.employeemanager.employee.EmployeeListActivity;
import com.example.employeemanager.salary.SalaryListActivity;
import com.example.employeemanager.util.Constants;

/** Tổng quan quản trị với công thực tế và các nhóm lương tách biệt. */
public class AdminDashboardActivity extends ReportActivity {
    @Override protected String requiredRole() { return Constants.ROLE_ADMIN; }
    @Override protected int titleResource() { return R.string.bc_admin_title; }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        dashboard();
        navigate(R.id.bc_employees, EmployeeListActivity.class);
        navigate(R.id.bc_payroll, SalaryListActivity.class);
    }
    @Override protected String reportText(int month, int year) {
        CompanySummary summary = reports.getAdminMonthlySummary(month, year);
        return getString(R.string.bc_company_summary, summary.employeeCount, hours(summary.totalMinutes / 60.0),
                money(summary.getCalculatedSalary()), money(summary.draftSalary), money(summary.paidSalary));
    }
}
