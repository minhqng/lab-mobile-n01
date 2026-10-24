package com.example.employeemanager.data.dao;

import androidx.room.Dao;
import androidx.room.Query;

/**
 * Truy vấn tổng hợp phục vụ báo cáo và tổng quan quản trị.
 *
 * <p>Đây là bộ truy vấn nền tảng ban đầu để module báo cáo mở rộng. Các phương thức
 * không trả về Entity mới và không tạo thêm bảng nghiệp vụ. Tham số {@code monthKey}
 * dùng định dạng {@code yyyy-MM}. Mọi phương thức phải được gọi trên luồng nền.</p>
 */
@Dao
public interface ReportDao {

    @Query("SELECT * FROM WorkLog WHERE status = 'COMPLETED' "
            + "AND substr(workDate, 1, 7) = :monthKey ORDER BY workDate, employeeId")
    java.util.List<com.example.employeemanager.data.entities.WorkLog> completedLogsByMonth(String monthKey);

    @Query("SELECT COUNT(*) FROM Employee")
    long countEmployees();

    /**
     * Tổng giờ làm đã hoàn tất của toàn công ty trong tháng.
     * Chỉ cộng bản ghi COMPLETED để không phụ thuộc việc Admin đã tạo bảng lương hay chưa.
     */
    @Query("SELECT COALESCE(SUM(totalHours), 0) FROM WorkLog "
            + "WHERE status = 'COMPLETED' AND substr(workDate, 1, 7) = :monthKey")
    double sumCompletedHoursByMonth(String monthKey);

    @Query("SELECT COALESCE(SUM(totalHours), 0) FROM WorkLog "
            + "WHERE employeeId = :employeeId AND status = 'COMPLETED' "
            + "AND substr(workDate, 1, 7) = :monthKey")
    double sumCompletedHoursByEmployeeAndMonth(long employeeId, String monthKey);

    /**
     * Tổng tiền lương theo trạng thái cụ thể, dùng cho báo cáo DRAFT hoặc PAID riêng biệt.
     */
    @Query("SELECT COALESCE(SUM(totalSalary), 0) FROM Salary "
            + "WHERE month = :month AND year = :year AND status = :status")
    long sumSalaryByMonthAndStatus(int month, int year, String status);

    /**
     * Tổng tiền lương đã tính trong tháng, gồm cả DRAFT và PAID.
     */
    @Query("SELECT COALESCE(SUM(totalSalary), 0) FROM Salary "
            + "WHERE month = :month AND year = :year")
    long sumSalaryByMonth(int month, int year);

    @Query("SELECT COUNT(*) FROM Salary WHERE month = :month AND year = :year")
    long countSalariesByMonth(int month, int year);
}
