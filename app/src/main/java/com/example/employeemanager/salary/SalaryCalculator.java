package com.example.employeemanager.salary;

import com.example.employeemanager.data.dto.SalaryBreakdown;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Lớp tính toán lương thuần Java, độc lập với cơ sở dữ liệu và giao diện.
 *
 * <p>Quy tắc tính toán:
 * <ul>
 *     <li>Số phút làm việc chuẩn mỗi ngày: tối đa 480 phút (8 giờ).</li>
 *     <li>Tăng ca tính riêng cho từng ngày, phần vượt quá 480 phút là giờ tăng ca.</li>
 *     <li>Hệ số tăng ca: 1.5.</li>
 *     <li>Tiền tính bằng {@link BigDecimal}, làm tròn HALF_UP ở bước cuối cùng đến đồng.</li>
 * </ul>
 * </p>
 */
public class SalaryCalculator {

    private static final long STANDARD_MINUTES_PER_DAY = Constants.STANDARD_MINUTES_PER_DAY;
    private static final BigDecimal OVERTIME_MULTIPLIER = BigDecimal.valueOf(15).divide(BigDecimal.valueOf(10));
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    /**
     * Tính toán phân rã giờ làm việc và tiền lương từ danh sách các bản ghi chấm công.
     *
     * @param logs       danh sách bản ghi chấm công trong tháng (phải ở trạng thái COMPLETED).
     * @param hourlyRate đơn giá giờ làm việc (VND/giờ, phải lớn hơn 0).
     * @return đối tượng {@link SalaryBreakdown} chứa đầy đủ thông tin phân rã.
     * @throws IllegalArgumentException nếu đơn giá giờ không hợp lệ hoặc bản ghi chấm công có dữ liệu sai.
     */
    public SalaryBreakdown calculateSalaryBreakdown(List<WorkLog> logs, long hourlyRate) {
        if (hourlyRate <= 0) {
            throw new IllegalArgumentException("Đơn giá giờ phải lớn hơn 0");
        }

        if (logs == null || logs.isEmpty()) {
            return new SalaryBreakdown(0L, 0L, 0L, 0.0, 0.0, 0L, hourlyRate);
        }

        long totalMinutes = 0L;
        long totalStandardMinutes = 0L;
        long totalOvertimeMinutes = 0L;

        for (WorkLog log : logs) {
            if (log == null) {
                throw new IllegalArgumentException("Bản ghi chấm công không được để trống (null)");
            }

            if (!Constants.STATUS_COMPLETED.equals(log.status)) {
                throw new IllegalArgumentException("Bản ghi chấm công có trạng thái không hợp lệ: " + log.status);
            }

            if (log.checkInTime == null || log.checkOutTime == null) {
                throw new IllegalArgumentException("Bản ghi chấm công thiếu giờ vào hoặc giờ ra");
            }

            LocalTime inTime;
            LocalTime outTime;
            try {
                inTime = LocalTime.parse(log.checkInTime);
                outTime = LocalTime.parse(log.checkOutTime);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Định dạng giờ chấm công không hợp lệ: " + e.getMessage(), e);
            }

            if (!outTime.isAfter(inTime)) {
                throw new IllegalArgumentException("Giờ chấm công ra (" + log.checkOutTime
                        + ") phải sau giờ chấm công vào (" + log.checkInTime + ")");
            }

            long dayMinutes = Duration.between(inTime, outTime).toMinutes();
            if (dayMinutes <= 0) {
                throw new IllegalArgumentException("Thời gian làm việc trong ngày dưới 1 phút hoàn chỉnh");
            }

            long dayStandardMinutes = Math.min(dayMinutes, STANDARD_MINUTES_PER_DAY);
            long dayOvertimeMinutes = Math.max(0L, dayMinutes - STANDARD_MINUTES_PER_DAY);

            totalMinutes += dayMinutes;
            totalStandardMinutes += dayStandardMinutes;
            totalOvertimeMinutes += dayOvertimeMinutes;
        }

        double totalHours = totalMinutes / 60.0;
        double overtimeHours = totalOvertimeMinutes / 60.0;

        BigDecimal regularMoney = BigDecimal.valueOf(totalStandardMinutes)
                .multiply(BigDecimal.valueOf(hourlyRate));

        BigDecimal overtimeMoney = BigDecimal.valueOf(totalOvertimeMinutes)
                .multiply(BigDecimal.valueOf(hourlyRate))
                .multiply(OVERTIME_MULTIPLIER);

        BigDecimal totalMinuteMoney = regularMoney.add(overtimeMoney);

        BigDecimal unroundedSalary = totalMinuteMoney.divide(MINUTES_PER_HOUR, 10, RoundingMode.HALF_UP);
        BigDecimal roundedSalary = unroundedSalary.setScale(0, RoundingMode.HALF_UP);

        long totalSalaryVnd = roundedSalary.longValueExact();

        return new SalaryBreakdown(
                totalMinutes,
                totalStandardMinutes,
                totalOvertimeMinutes,
                totalHours,
                overtimeHours,
                totalSalaryVnd,
                hourlyRate
        );
    }
}
