package com.example.employeemanager.data.dto;

import java.util.Objects;

/**
 * Đối tượng truyền dữ liệu (DTO) biểu diễn chi tiết phân rã giờ làm việc và tiền lương.
 *
 * <p>Đây là lớp DTO phục vụ tính toán và hiển thị, không phải là bảng trong Room database.</p>
 */
public class SalaryBreakdown {

    private final long totalMinutes;
    private final long standardMinutes;
    private final long overtimeMinutes;
    private final double totalHours;
    private final double overtimeHours;
    private final long totalSalary;
    private final long hourlyRate;

    public SalaryBreakdown(long totalMinutes,
                           long standardMinutes,
                           long overtimeMinutes,
                           double totalHours,
                           double overtimeHours,
                           long totalSalary,
                           long hourlyRate) {
        this.totalMinutes = totalMinutes;
        this.standardMinutes = standardMinutes;
        this.overtimeMinutes = overtimeMinutes;
        this.totalHours = totalHours;
        this.overtimeHours = overtimeHours;
        this.totalSalary = totalSalary;
        this.hourlyRate = hourlyRate;
    }

    /**
     * Tổng số phút làm việc trong tháng.
     */
    public long getTotalMinutes() {
        return totalMinutes;
    }

    /**
     * Tổng số phút làm việc giờ thường trong tháng (tối đa 480 phút mỗi ngày).
     */
    public long getStandardMinutes() {
        return standardMinutes;
    }

    /**
     * Tổng số phút làm việc tăng ca trong tháng (phần vượt quá 480 phút mỗi ngày).
     */
    public long getOvertimeMinutes() {
        return overtimeMinutes;
    }

    /**
     * Tổng số giờ làm việc trong tháng (đã bao gồm giờ tăng ca).
     */
    public double getTotalHours() {
        return totalHours;
    }

    /**
     * Tổng số giờ tăng ca trong tháng.
     */
    public double getOvertimeHours() {
        return overtimeHours;
    }

    /**
     * Tổng tiền lương VND (đã làm tròn HALF_UP đến đồng cuối cùng).
     */
    public long getTotalSalary() {
        return totalSalary;
    }

    /**
     * Đơn giá giờ làm việc (VND/giờ).
     */
    public long getHourlyRate() {
        return hourlyRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SalaryBreakdown that = (SalaryBreakdown) o;
        return totalMinutes == that.totalMinutes
                && standardMinutes == that.standardMinutes
                && overtimeMinutes == that.overtimeMinutes
                && Double.compare(that.totalHours, totalHours) == 0
                && Double.compare(that.overtimeHours, overtimeHours) == 0
                && totalSalary == that.totalSalary
                && hourlyRate == that.hourlyRate;
    }

    @Override
    public int hashCode() {
        return Objects.hash(totalMinutes, standardMinutes, overtimeMinutes, totalHours, overtimeHours, totalSalary, hourlyRate);
    }

    @Override
    public String toString() {
        return "SalaryBreakdown{"
                + "totalMinutes=" + totalMinutes
                + ", standardMinutes=" + standardMinutes
                + ", overtimeMinutes=" + overtimeMinutes
                + ", totalHours=" + totalHours
                + ", overtimeHours=" + overtimeHours
                + ", totalSalary=" + totalSalary
                + ", hourlyRate=" + hourlyRate
                + '}';
    }
}
