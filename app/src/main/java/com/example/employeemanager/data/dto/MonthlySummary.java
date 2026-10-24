package com.example.employeemanager.data.dto;

/** Tổng công hoàn tất, không phụ thuộc việc đã tạo bảng lương. */
public final class MonthlySummary {
    public final long totalMinutes;
    public final long overtimeMinutes;
    public final int completedDays;
    public MonthlySummary(long totalMinutes, long overtimeMinutes, int completedDays) {
        this.totalMinutes = totalMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.completedDays = completedDays;
    }
    public double getTotalHours() { return totalMinutes / 60.0; }
    public double getOvertimeHours() { return overtimeMinutes / 60.0; }
}
