package com.example.employeemanager.attendance;

import static org.junit.Assert.assertEquals;

import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AttendanceSummaryCalculatorTest {

    @Test public void tongKetLayPhutTuGioGocKhongTuSoGioDaLuu() {
        WorkLog log = new WorkLog(1, "2026-09-01", "08:00:10", "18:00:05", 0,
                Constants.STATUS_COMPLETED);
        assertEquals(599, AttendanceSummaryCalculator.completedMinutes(java.util.Collections.singletonList(log)));
        assertEquals(119, AttendanceSummaryCalculator.overtimeMinutes(java.util.Collections.singletonList(log)));
    }

    @Test
    public void openLogsAreExcludedFromTotals() {
        List<WorkLog> logs = new ArrayList<>();
        WorkLog completed = new WorkLog(1, "2026-10-01", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED);
        completed.id = 1;
        logs.add(completed);
        WorkLog open = new WorkLog(1, "2026-10-02", "08:00:00", null, 0, Constants.STATUS_OPEN);
        open.id = 2;
        logs.add(open);

        assertEquals(480, AttendanceSummaryCalculator.completedMinutes(logs));
        assertEquals(0, AttendanceSummaryCalculator.overtimeMinutes(logs));
    }
}
