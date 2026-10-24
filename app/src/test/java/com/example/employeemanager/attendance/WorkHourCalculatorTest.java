package com.example.employeemanager.attendance;

import static org.junit.Assert.assertEquals;

import com.example.employeemanager.util.Constants;

import org.junit.Test;

import java.time.LocalTime;

/**
 * Kiểm thử hàm tính phút và giờ làm việc.
 */
public class WorkHourCalculatorTest {

    @Test
    public void eightHoursHasNoOvertime() {
        assertEquals(480, WorkHourCalculator.calculateWorkMinutes(
                LocalTime.of(8, 0, 0), LocalTime.of(16, 0, 0)));
        assertEquals(0, WorkHourCalculator.overtimeMinutes(480));
    }

    @Test
    public void tenHoursHasTwoOvertimeHours() {
        assertEquals(600, WorkHourCalculator.calculateWorkMinutes(
                LocalTime.of(8, 0, 0), LocalTime.of(18, 0, 0)));
        assertEquals(120, WorkHourCalculator.overtimeMinutes(600));
    }

    @Test
    public void partialMinuteIsTruncated() {
        assertEquals(0, WorkHourCalculator.calculateWorkMinutes(
                LocalTime.of(8, 0, 10), LocalTime.of(8, 1, 5)));
    }

    @Test
    public void overtimeThresholdIsEightHours() {
        assertEquals(Constants.STANDARD_MINUTES_PER_DAY,
                WorkHourCalculator.standardMinutesPerDay());
        assertEquals(420, WorkHourCalculator.regularMinutes(420));
        assertEquals(480, WorkHourCalculator.regularMinutes(600));
        assertEquals(120, WorkHourCalculator.overtimeMinutes(600));
    }
}
