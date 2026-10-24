package com.example.employeemanager.attendance;

import android.database.sqlite.SQLiteConstraintException;
import com.example.employeemanager.data.AppDB;

import com.example.employeemanager.data.dao.SalaryDao;
import com.example.employeemanager.data.dao.WorkLogDao;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;
import com.example.employeemanager.util.DateTimeUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Nghiệp vụ chấm công vào, chấm công ra, xem trạng thái và lịch sử.
 *
 * <p>Lớp này không chạy SQL trực tiếp; mọi truy vấn đi qua DAO. Đồng hồ có thể tiêm vào
 * để kiểm thử mà không phải chờ thời gian thực.</p>
 */
public class AttendanceService {

    private final WorkLogDao workLogDao;
    private final SalaryDao salaryDao;
    private final SessionProvider sessionProvider;
    private Clock clock;
    private AppDB database;

    public AttendanceService(AppDB database, SessionProvider sessionProvider, Clock clock) {
        this(database.workLogDao(), database.salaryDao(), sessionProvider, clock);
        this.database = database;
    }

    public AttendanceService(WorkLogDao workLogDao) {
        this(workLogDao, null, null, DateTimeUtils.systemClock());
    }

    public AttendanceService(WorkLogDao workLogDao, Clock clock) {
        this(workLogDao, null, null, clock);
    }

    public AttendanceService(WorkLogDao workLogDao, SessionProvider sessionProvider, Clock clock) {
        this(workLogDao, null, sessionProvider, clock);
    }

    public AttendanceService(WorkLogDao workLogDao, SalaryDao salaryDao,
                             SessionProvider sessionProvider, Clock clock) {
        if (workLogDao == null) {
            throw new IllegalArgumentException("WorkLogDao không được để trống");
        }
        this.workLogDao = workLogDao;
        this.salaryDao = salaryDao;
        this.sessionProvider = sessionProvider;
        this.clock = (clock == null) ? DateTimeUtils.systemClock() : clock;
    }

    public void setClock(Clock clock) {
        this.clock = (clock == null) ? DateTimeUtils.systemClock() : clock;
    }

    public Clock getClock() {
        return clock;
    }

    public WorkLog checkIn() {
        if (database != null) {
            return database.runInTransaction(() -> checkIn(requireEmployeeId()));
        }
        return checkIn(requireEmployeeId());
    }

    WorkLog checkIn(long employeeId) {
        validateEmployeeId(employeeId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now(DateTimeUtils.withZone(clock));
        LocalDate today = now.toLocalDate();
        String todayKey = today.toString();
        if (isSalaryPaid(employeeId, today.getMonthValue(), today.getYear())) {
            throw new AttendanceException("Tháng này đã chốt lương, không thể chấm công");
        }

        WorkLog openLog = workLogDao.findOpenByEmployee(employeeId);
        if (openLog != null) {
            throw new AttendanceException("Bạn có lượt chấm công chưa hoàn tất");
        }

        WorkLog todayLog = workLogDao.findByEmployeeAndDate(employeeId, todayKey);
        if (todayLog != null) {
            throw new AttendanceException("Bạn đã chấm công vào hôm nay");
        }

        String checkInTime = now.toLocalTime().format(java.time.format.DateTimeFormatter.ofPattern(Constants.TIME_STORAGE_PATTERN));
        WorkLog workLog = new WorkLog(
                employeeId, todayKey, checkInTime, null, 0.0, Constants.STATUS_OPEN);

        try {
            long id = workLogDao.insert(workLog);
            workLog.id = id;
        } catch (SQLiteConstraintException exception) {
            throw new AttendanceException("Bạn đã chấm công vào hôm nay", exception);
        } catch (RuntimeException exception) {
            throw new AttendanceException(
                    "Không thể lưu dữ liệu. Vui lòng kiểm tra và thử lại", exception);
        }
        return workLog;
    }

    public WorkLog checkOut() {
        if (database != null) {
            return database.runInTransaction(() -> checkOut(requireEmployeeId()));
        }
        return checkOut(requireEmployeeId());
    }

    WorkLog checkOut(long employeeId) {
        validateEmployeeId(employeeId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now(DateTimeUtils.withZone(clock));
        LocalDate today = now.toLocalDate();
        String todayKey = today.toString();

        WorkLog todayLog = workLogDao.findByEmployeeAndDate(employeeId, todayKey);
        if (todayLog == null) {
            WorkLog openLog = workLogDao.findOpenByEmployee(employeeId);
            if (openLog != null) {
                throw new AttendanceException("Ứng dụng chưa hỗ trợ ca làm qua ngày");
            }
            throw new AttendanceException("Bạn chưa chấm công vào hôm nay");
        }

        if (Constants.STATUS_COMPLETED.equals(todayLog.status)
                || todayLog.checkOutTime != null) {
            throw new AttendanceException("Bạn đã chấm công ra hôm nay");
        }

        if (!Constants.STATUS_OPEN.equals(todayLog.status)) {
            throw new AttendanceException("Bản ghi chấm công không hợp lệ");
        }

        if (isSalaryPaid(employeeId, today.getMonthValue(), today.getYear())) {
            throw new AttendanceException("Tháng này đã chốt lương, không thể chấm công");
        }

        LocalTime checkInTime = DateTimeUtils.parseTime(todayLog.checkInTime);
        LocalTime checkOutNow = now.toLocalTime().withNano(0);
        long workedMinutes = WorkHourCalculator.calculateWorkMinutes(checkInTime, checkOutNow);

        if (workedMinutes <= 0) {
            throw new AttendanceException("Giờ chấm công ra phải sau giờ vào");
        }

        String checkOutTime = checkOutNow.format(java.time.format.DateTimeFormatter.ofPattern(Constants.TIME_STORAGE_PATTERN));
        double totalHours = WorkHourCalculator.minutesToHours(workedMinutes);

        int updatedRows;
        try {
            updatedRows = workLogDao.updateCheckoutIfOpen(
                    todayLog.id, checkOutTime, totalHours, Constants.STATUS_COMPLETED);
        } catch (RuntimeException exception) {
            throw new AttendanceException("Không thể lưu dữ liệu. Vui lòng kiểm tra và thử lại", exception);
        }
        if (updatedRows != 1) {
            throw new AttendanceException("Bản ghi chấm công không còn ở trạng thái đang làm việc");
        }

        todayLog.checkOutTime = checkOutTime;
        todayLog.totalHours = totalHours;
        todayLog.status = Constants.STATUS_COMPLETED;
        return todayLog;
    }

    public WorkLog getTodayWorkLog() {
        return getTodayWorkLog(requireEmployeeId());
    }

    WorkLog getTodayWorkLog(long employeeId) {
        validateEmployeeId(employeeId);
        return workLogDao.findByEmployeeAndDate(employeeId, DateTimeUtils.todayString(clock));
    }

    public List<WorkLog> getAttendanceHistory(int month, int year) {
        return getAttendanceHistory(requireEmployeeId(), month, year);
    }

    List<WorkLog> getAttendanceHistory(long employeeId, int month, int year) {
        validateEmployeeId(employeeId);
        validateMonthYear(month, year);
        return workLogDao.findByEmployeeAndMonth(employeeId, DateTimeUtils.monthKey(year, month));
    }

    public List<WorkLog> getCompletedLogsByMonth(long employeeId, int month, int year) {
        if (requireEmployeeId() != employeeId) {
            throw new SecurityException("Bạn không có quyền xem thông tin này");
        }
        validateEmployeeId(employeeId);
        validateMonthYear(month, year);
        return workLogDao.findCompletedByEmployeeAndMonth(
                employeeId, DateTimeUtils.monthKey(year, month));
    }

    public long calculateWorkMinutes(LocalTime start, LocalTime end) {
        return WorkHourCalculator.calculateWorkMinutes(start, end);
    }

    public static long calculateWorkMinutesStatic(LocalTime start, LocalTime end) {
        return WorkHourCalculator.calculateWorkMinutes(start, end);
    }

    private long requireEmployeeId() {
        if (sessionProvider == null) {
            throw new AttendanceException("Chưa có phiên đăng nhập");
        }
        long employeeId = sessionProvider.requireCurrentEmployeeId();
        if (employeeId <= 0) {
            throw new AttendanceException("Chưa có phiên đăng nhập");
        }
        return employeeId;
    }

    private void validateEmployeeId(long employeeId) {
        if (employeeId <= 0) {
            throw new IllegalArgumentException("Mã nhân viên không hợp lệ");
        }
    }

    private void validateMonthYear(int month, int year) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Tháng phải từ 1 đến 12");
        }
        if (year <= 0) {
            throw new IllegalArgumentException("Năm không hợp lệ");
        }
    }

    private boolean isSalaryPaid(long employeeId, int month, int year) {
        if (salaryDao == null) {
            return false;
        }
        Salary salary = salaryDao.findByEmployeeAndMonth(employeeId, month, year);
        return salary != null && Constants.STATUS_PAID.equals(salary.status);
    }
}
