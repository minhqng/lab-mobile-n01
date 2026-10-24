package com.example.employeemanager.reports;

import androidx.annotation.WorkerThread;
import com.example.employeemanager.attendance.AttendanceSummaryCalculator;
import com.example.employeemanager.auth.SessionManager;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.dto.CompanySummary;
import com.example.employeemanager.data.dto.MonthlySummary;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;
import java.util.List;
import java.util.Locale;

/** Kiểm tra quyền trong mỗi ảnh chụp dữ liệu, không nhận mã nhân viên từ UI. */
@WorkerThread
public final class ReportService {
    private final AppDB database;
    private final SessionManager sessions;
    public ReportService(AppDB database, SessionManager sessions) {
        this.database = database;
        this.sessions = sessions;
    }
    private String period(int month, int year) {
        if (year < 1 || year > 9999 || month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng hoặc năm không hợp lệ");
        }
        return String.format(Locale.ROOT, "%04d-%02d", year, month);
    }
    public Employee getMyEmployee() {
        return database.runInTransaction(() -> database.employeeDao()
                .findById(sessions.requireCurrentEmployeeId()));
    }
    public MonthlySummary getMyMonthlySummary(int month, int year) {
        String key = period(month, year);
        return database.runInTransaction(() -> {
            long employeeId = sessions.requireCurrentEmployeeId();
            List<WorkLog> logs = database.workLogDao().findCompletedByEmployeeAndMonth(employeeId, key);
            return new MonthlySummary(AttendanceSummaryCalculator.completedMinutes(logs),
                    AttendanceSummaryCalculator.overtimeMinutes(logs), logs.size());
        });
    }
    public Salary getMySalary(int month, int year) {
        period(month, year);
        return database.runInTransaction(() -> database.salaryDao().findByEmployeeAndMonth(
                sessions.requireCurrentEmployeeId(), month, year));
    }
    public CompanySummary getAdminMonthlySummary(int month, int year) {
        String key = period(month, year);
        return database.runInTransaction(() -> {
            sessions.requireRole(Constants.ROLE_ADMIN);
            long minutes = AttendanceSummaryCalculator.completedMinutes(database.reportDao().completedLogsByMonth(key));
            long draft = 0, paid = 0;
            for (Salary salary : database.salaryDao().findByMonth(month, year)) {
                if (Constants.STATUS_DRAFT.equals(salary.status)) draft = Math.addExact(draft, salary.totalSalary);
                else if (Constants.STATUS_PAID.equals(salary.status)) paid = Math.addExact(paid, salary.totalSalary);
                else throw new IllegalStateException("Trạng thái bảng lương không hợp lệ");
            }
            return new CompanySummary(database.reportDao().countEmployees(), minutes, draft, paid);
        });
    }
}
