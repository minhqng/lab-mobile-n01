package com.example.employeemanager.attendance;

import com.example.employeemanager.util.Constants;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Hàm thuần dùng để tính số phút và giờ làm việc.
 *
 * <p>Các phương thức ở đây không phụ thuộc Activity, DAO hay thời gian thực.</p>
 */
public final class WorkHourCalculator {

    private WorkHourCalculator() {
        // Lớp tiện ích không được khởi tạo.
    }

    /**
     * Tính số phút hoàn chỉnh giữa hai thời điểm, làm tròn xuống phút.
     *
     * <p>Nếu giờ kết thúc không muộn hơn giờ bắt đầu, kết quả có thể bằng 0 hoặc âm;
     * lớp nghiệp vụ phải từ chối các trường hợp không hợp lệ.</p>
     */
    public static long calculateWorkMinutes(LocalTime start, LocalTime end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Giờ bắt đầu và giờ kết thúc không được để trống");
        }
        return Duration.between(start, end).toMinutes();
    }

    /**
     * Quy đổi số phút thành số giờ.
     */
    public static double minutesToHours(long minutes) {
        return minutes / 60.0;
    }

    /**
     * Số phút tiêu chuẩn trong một ngày làm việc.
     */
    public static long standardMinutesPerDay() {
        return Constants.STANDARD_MINUTES_PER_DAY;
    }

    /**
     * Số phút thường của một ngày, không vượt quá 8 giờ.
     */
    public static long regularMinutes(long workedMinutes) {
        return Math.min(Math.max(workedMinutes, 0L), Constants.STANDARD_MINUTES_PER_DAY);
    }

    /**
     * Số phút tăng ca của một ngày.
     */
    public static long overtimeMinutes(long workedMinutes) {
        return Math.max(0L, workedMinutes - Constants.STANDARD_MINUTES_PER_DAY);
    }
}