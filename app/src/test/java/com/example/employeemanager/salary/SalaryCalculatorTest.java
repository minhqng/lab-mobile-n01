package com.example.employeemanager.salary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import com.example.employeemanager.data.dto.SalaryBreakdown;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Kiểm thử đơn vị cho thuật toán tính lương trong {@link SalaryCalculator}.
 */
public class SalaryCalculatorTest {

    private static final double DELTA = 0.0001;
    private SalaryCalculator calculator;

    @Before
    public void setUp() {
        calculator = new SalaryCalculator();
    }

    private WorkLog createCompletedLog(String date, String inTime, String outTime) {
        WorkLog log = new WorkLog();
        log.employeeId = 1L;
        log.workDate = date;
        log.checkInTime = inTime;
        log.checkOutTime = outTime;
        log.status = Constants.STATUS_COMPLETED;
        return log;
    }

    @Test
    public void testEightHours_NoOvertime_CorrectSalary() {
        // 08:00:00 -> 16:00:00 = 480 phút = 8 giờ thường, 0 giờ OT, đơn giá 50.000đ -> 400.000đ
        List<WorkLog> logs = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "16:00:00")
        );

        SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(logs, 50_000L);

        assertEquals(480L, breakdown.getTotalMinutes());
        assertEquals(480L, breakdown.getStandardMinutes());
        assertEquals(0L, breakdown.getOvertimeMinutes());
        assertEquals(8.0, breakdown.getTotalHours(), DELTA);
        assertEquals(0.0, breakdown.getOvertimeHours(), DELTA);
        assertEquals(400_000L, breakdown.getTotalSalary());
    }

    @Test
    public void testTenHours_TwoHoursOvertime_CorrectSalary() {
        // 08:00:00 -> 18:00:00 = 600 phút = 480 phút thường + 120 phút OT.
        // Đơn giá 50.000đ -> Thường: 400.000đ, OT: 2 * 50.000 * 1.5 = 150.000đ -> Tổng 550.000đ
        List<WorkLog> logs = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "18:00:00")
        );

        SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(logs, 50_000L);

        assertEquals(600L, breakdown.getTotalMinutes());
        assertEquals(480L, breakdown.getStandardMinutes());
        assertEquals(120L, breakdown.getOvertimeMinutes());
        assertEquals(10.0, breakdown.getTotalHours(), DELTA);
        assertEquals(2.0, breakdown.getOvertimeHours(), DELTA);
        assertEquals(550_000L, breakdown.getTotalSalary());
    }

    @Test
    public void testSixHours_NoOvertime_CorrectSalary() {
        // 08:00:00 -> 14:00:00 = 360 phút = 6 giờ thường, 0 giờ OT, đơn giá 50.000đ -> 300.000đ
        List<WorkLog> logs = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "14:00:00")
        );

        SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(logs, 50_000L);

        assertEquals(360L, breakdown.getTotalMinutes());
        assertEquals(360L, breakdown.getStandardMinutes());
        assertEquals(0L, breakdown.getOvertimeMinutes());
        assertEquals(6.0, breakdown.getTotalHours(), DELTA);
        assertEquals(0.0, breakdown.getOvertimeHours(), DELTA);
        assertEquals(300_000L, breakdown.getTotalSalary());
    }

    @Test
    public void testStandardMonthlyScenario_TwentyTwoDays_180Hours_4HoursOT_9100000Vnd() {
        // 22 ngày: 20 ngày 8h (480 phút) + 2 ngày 10h (600 phút)
        // Tổng phút = 20 * 480 + 2 * 600 = 9600 + 1200 = 10800 phút = 180 giờ
        // Phút thường = 20 * 480 + 2 * 480 = 10560 phút = 176 giờ
        // Phút OT = 2 * 120 = 240 phút = 4 giờ
        // Lương = 176 * 50.000 + 4 * 50.000 * 1.5 = 8.800.000 + 300.000 = 9.100.000đ
        List<WorkLog> logs = new ArrayList<>();
        for (int i = 1; i <= 20; i++) {
            String date = String.format("2026-09-%02d", i);
            logs.add(createCompletedLog(date, "08:00:00", "16:00:00"));
        }
        logs.add(createCompletedLog("2026-09-21", "08:00:00", "18:00:00"));
        logs.add(createCompletedLog("2026-09-22", "08:00:00", "18:00:00"));

        SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(logs, 50_000L);

        assertEquals(10_800L, breakdown.getTotalMinutes());
        assertEquals(10_560L, breakdown.getStandardMinutes());
        assertEquals(240L, breakdown.getOvertimeMinutes());
        assertEquals(180.0, breakdown.getTotalHours(), DELTA);
        assertEquals(4.0, breakdown.getOvertimeHours(), DELTA);
        assertEquals(9_100_000L, breakdown.getTotalSalary());
    }

    @Test
    public void testBoundary_480MinutesAnd481Minutes() {
        // Biên 480 phút: chính xác 8 giờ thường, 0 OT
        List<WorkLog> logs480 = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "16:00:00")
        );
        SalaryBreakdown bd480 = calculator.calculateSalaryBreakdown(logs480, 50_000L);
        assertEquals(480L, bd480.getTotalMinutes());
        assertEquals(480L, bd480.getStandardMinutes());
        assertEquals(0L, bd480.getOvertimeMinutes());

        // Biên 481 phút: 480 phút thường + 1 phút OT
        // Lương: (480 * 50.000 + 1 * 50.000 * 1.5) / 60 = (24.000.000 + 75.000) / 60 = 24.075.000 / 60 = 401.250đ
        List<WorkLog> logs481 = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "16:01:00")
        );
        SalaryBreakdown bd481 = calculator.calculateSalaryBreakdown(logs481, 50_000L);
        assertEquals(481L, bd481.getTotalMinutes());
        assertEquals(480L, bd481.getStandardMinutes());
        assertEquals(1L, bd481.getOvertimeMinutes());
        assertEquals(481.0 / 60.0, bd481.getTotalHours(), DELTA);
        assertEquals(1.0 / 60.0, bd481.getOvertimeHours(), DELTA);
        assertEquals(401_250L, bd481.getTotalSalary());
    }

    @Test
    public void testEmptyOrNullLogs_ReturnsZero() {
        SalaryBreakdown bdEmpty = calculator.calculateSalaryBreakdown(Collections.emptyList(), 50_000L);
        assertNotNull(bdEmpty);
        assertEquals(0L, bdEmpty.getTotalMinutes());
        assertEquals(0L, bdEmpty.getStandardMinutes());
        assertEquals(0L, bdEmpty.getOvertimeMinutes());
        assertEquals(0.0, bdEmpty.getTotalHours(), DELTA);
        assertEquals(0.0, bdEmpty.getOvertimeHours(), DELTA);
        assertEquals(0L, bdEmpty.getTotalSalary());

        SalaryBreakdown bdNull = calculator.calculateSalaryBreakdown(null, 50_000L);
        assertNotNull(bdNull);
        assertEquals(0L, bdNull.getTotalMinutes());
        assertEquals(0L, bdNull.getTotalSalary());
    }

    @Test
    public void testInvalidRate_ThrowsException() {
        List<WorkLog> logs = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "16:00:00")
        );

        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(logs, 0L)
        );

        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(logs, -10_000L)
        );
    }

    @Test
    public void testInvalidLogs_ThrowsException() {
        // Log null trong danh sách
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(Collections.singletonList(null), 50_000L)
        );

        // Trạng thái OPEN
        WorkLog openLog = createCompletedLog("2026-09-01", "08:00:00", "16:00:00");
        openLog.status = Constants.STATUS_OPEN;
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(Collections.singletonList(openLog), 50_000L)
        );

        // Thiếu checkOutTime
        WorkLog missingOutLog = createCompletedLog("2026-09-01", "08:00:00", null);
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(Collections.singletonList(missingOutLog), 50_000L)
        );

        // Thiếu checkInTime
        WorkLog missingInLog = createCompletedLog("2026-09-01", null, "16:00:00");
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(Collections.singletonList(missingInLog), 50_000L)
        );

        // Giờ ra <= giờ vào
        WorkLog outBeforeInLog = createCompletedLog("2026-09-01", "16:00:00", "08:00:00");
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(Collections.singletonList(outBeforeInLog), 50_000L)
        );

        // Thời gian làm dưới 1 phút hoàn chỉnh
        WorkLog underOneMinuteLog = createCompletedLog("2026-09-01", "08:00:10", "08:00:45");
        assertThrows(IllegalArgumentException.class, () ->
                calculator.calculateSalaryBreakdown(Collections.singletonList(underOneMinuteLog), 50_000L)
        );
    }

    @Test
    public void testRoundingHalfUp_HalfDongRoundsUp() {
        // 30 phút với đơn giá 50.001đ:
        // Lương chưa làm tròn = (30 * 50.001) / 60 = 1.500.030 / 60 = 25.000,5đ
        // Làm tròn HALF_UP đến đồng: 25.001đ
        List<WorkLog> logs = Collections.singletonList(
                createCompletedLog("2026-09-01", "08:00:00", "08:30:00")
        );

        SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(logs, 50_001L);

        assertEquals(30L, breakdown.getTotalMinutes());
        assertEquals(30L, breakdown.getStandardMinutes());
        assertEquals(0L, breakdown.getOvertimeMinutes());
        assertEquals(25_001L, breakdown.getTotalSalary());
    }

    @Test
    public void testMultipleLogs_OvertimeCalculatedPerDayNotMonthlyThreshold() {
        // 2 ngày, mỗi ngày 6 giờ (360 phút).
        // Tổng tháng = 720 phút = 12 giờ.
        // Vì mỗi ngày <= 480 phút, OT của từng ngày = 0 -> Tổng OT = 0.
        // Nếu ai đó tính OT sai bằng cách so sánh tổng tháng với một ngưỡng sẽ sai.
        List<WorkLog> logs = new ArrayList<>();
        logs.add(createCompletedLog("2026-09-01", "08:00:00", "14:00:00")); // 6h
        logs.add(createCompletedLog("2026-09-02", "08:00:00", "14:00:00")); // 6h

        SalaryBreakdown breakdown = calculator.calculateSalaryBreakdown(logs, 50_000L);

        assertEquals(720L, breakdown.getTotalMinutes());
        assertEquals(720L, breakdown.getStandardMinutes());
        assertEquals(0L, breakdown.getOvertimeMinutes());
        assertEquals(12.0, breakdown.getTotalHours(), DELTA);
        assertEquals(0.0, breakdown.getOvertimeHours(), DELTA);
        assertEquals(600_000L, breakdown.getTotalSalary());
    }
}
