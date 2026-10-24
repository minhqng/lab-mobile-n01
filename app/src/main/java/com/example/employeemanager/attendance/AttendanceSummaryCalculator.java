package com.example.employeemanager.attendance;

import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import java.util.List;

/**
 * Tính tổng giờ và giờ tăng ca từ danh sách chấm công.
 *
 * <p>Chỉ bản ghi COMPLETED mới được cộng; bản ghi OPEN được hiển thị nhưng không tính công.</p>
 */
public final class AttendanceSummaryCalculator {

    private AttendanceSummaryCalculator() {
    }

    public static long completedMinutes(List<WorkLog> logs) {
        long total = 0;
        if (logs == null) {
            return total;
        }
        for (WorkLog log : logs) {
            if (log != null && Constants.STATUS_COMPLETED.equals(log.status)) {
                total += Math.max(0, Math.round(log.totalHours * 60.0));
            }
        }
        return total;
    }

    public static long overtimeMinutes(List<WorkLog> logs) {
        long total = 0;
        if (logs == null) {
            return total;
        }
        for (WorkLog log : logs) {
            if (log != null && Constants.STATUS_COMPLETED.equals(log.status)) {
                long minutes = Math.max(0, Math.round(log.totalHours * 60.0));
                total += WorkHourCalculator.overtimeMinutes(minutes);
            }
        }
        return total;
    }
}
