package com.example.employeemanager.data.dto;

/** Tổng lương đã tính bao gồm nháp và đã thanh toán; không cộng hai lần. */
public final class CompanySummary {
    public final long employeeCount;
    public final long totalMinutes;
    public final long draftSalary;
    public final long paidSalary;
    public CompanySummary(long employeeCount, long totalMinutes, long draftSalary, long paidSalary) {
        this.employeeCount = employeeCount;
        this.totalMinutes = totalMinutes;
        this.draftSalary = draftSalary;
        this.paidSalary = paidSalary;
    }
    public long getCalculatedSalary() { return Math.addExact(draftSalary, paidSalary); }
}
