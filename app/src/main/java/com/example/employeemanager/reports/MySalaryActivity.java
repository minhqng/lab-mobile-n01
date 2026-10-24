package com.example.employeemanager.reports;

import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.util.Constants;

/** Báo cáo lương không nhận employeeId, không tự tạo bảng lương. */
public class MySalaryActivity extends ReportActivity {
    @Override protected String requiredRole() { return Constants.ROLE_EMPLOYEE; }
    @Override protected int titleResource() { return R.string.bc_my_salary; }
    @Override protected String reportText(int month, int year) {
        Salary salary = reports.getMySalary(month, year);
        if (salary == null) return getString(R.string.bc_no_salary);
        return getString(R.string.bc_salary_summary, hours(salary.totalHours),
                hours(salary.totalHours - salary.overtimeHours), hours(salary.overtimeHours),
                money(salary.totalSalary), getString(Constants.STATUS_PAID.equals(salary.status)
                        ? R.string.luong_status_paid : R.string.luong_status_draft));
    }
}
