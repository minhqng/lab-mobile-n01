package com.example.employeemanager.attendance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.example.employeemanager.data.dao.SalaryDao;
import com.example.employeemanager.data.dao.WorkLogDao;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;
import com.example.employeemanager.util.DateTimeUtils;

import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm thử nghiệp vụ chấm công với đồng hồ cố định.
 */
public class AttendanceServiceTest {

    private static final ZoneId ZONE = ZoneId.of(Constants.TIME_ZONE_ID);

    private static Clock fixedClock(String instantText) {
        return Clock.fixed(Instant.parse(instantText), ZONE);
    }

    private static class FakeWorkLogDao implements WorkLogDao {
        final List<WorkLog> logs = new ArrayList<>();
        long nextId = 1;

        @Override
        public long insert(WorkLog workLog) {
            for (WorkLog existing : logs) {
                if (existing.employeeId == workLog.employeeId
                        && existing.workDate.equals(workLog.workDate)) {
                    throw new IllegalStateException("duplicate");
                }
            }
            workLog.id = nextId++;
            logs.add(workLog);
            return workLog.id;
        }

        @Override
        public void update(WorkLog workLog) {
            for (int i = 0; i < logs.size(); i++) {
                if (logs.get(i).id == workLog.id) {
                    logs.set(i, workLog);
                    return;
                }
            }
        }

        @Override
        public int updateCheckoutIfOpen(long id, String checkOutTime, double totalHours, String status) {
            for (int i = 0; i < logs.size(); i++) {
                WorkLog log = logs.get(i);
                if (log.id == id && Constants.STATUS_OPEN.equals(log.status)) {
                    log.checkOutTime = checkOutTime;
                    log.totalHours = totalHours;
                    log.status = status;
                    logs.set(i, log);
                    return 1;
                }
            }
            return 0;
        }

        @Override
        public void delete(WorkLog workLog) {
        }

        @Override
        public WorkLog findById(long workLogId) {
            for (WorkLog log : logs) {
                if (log.id == workLogId) {
                    return log;
                }
            }
            return null;
        }

        @Override
        public WorkLog findByEmployeeAndDate(long employeeId, String workDate) {
            for (WorkLog log : logs) {
                if (log.employeeId == employeeId && log.workDate.equals(workDate)) {
                    return log;
                }
            }
            return null;
        }

        @Override
        public WorkLog findOpenByEmployee(long employeeId) {
            for (WorkLog log : logs) {
                if (log.employeeId == employeeId && Constants.STATUS_OPEN.equals(log.status)) {
                    return log;
                }
            }
            return null;
        }

        @Override
        public WorkLog findOpenByEmployeeAndMonth(long employeeId, String monthKey) {
            for (WorkLog log : logs) {
                if (log.employeeId == employeeId && Constants.STATUS_OPEN.equals(log.status)
                        && log.workDate.startsWith(monthKey)) {
                    return log;
                }
            }
            return null;
        }

        @Override
        public List<WorkLog> findByEmployeeAndMonth(long employeeId, String monthKey) {
            List<WorkLog> result = new ArrayList<>();
            for (WorkLog log : logs) {
                if (log.employeeId == employeeId && log.workDate.startsWith(monthKey)) {
                    result.add(log);
                }
            }
            result.sort((a, b) -> b.workDate.compareTo(a.workDate));
            return result;
        }

        @Override
        public List<WorkLog> findCompletedByEmployeeAndMonth(long employeeId, String monthKey) {
            List<WorkLog> result = new ArrayList<>();
            for (WorkLog log : logs) {
                if (log.employeeId == employeeId && Constants.STATUS_COMPLETED.equals(log.status)
                        && log.workDate.startsWith(monthKey)) {
                    result.add(log);
                }
            }
            return result;
        }

        @Override
        public long countByEmployeeId(long employeeId) {
            long count = 0;
            for (WorkLog log : logs) {
                if (log.employeeId == employeeId) {
                    count++;
                }
            }
            return count;
        }
    }

    private static class FakeSalaryDao implements SalaryDao {
        Salary salary;

        @Override
        public long insert(Salary salary) {
            this.salary = salary;
            salary.id = 1;
            return 1;
        }

        @Override
        public void update(Salary salary) {
            this.salary = salary;
        }

        @Override
        public void delete(Salary salary) {
        }

        @Override
        public Salary findById(long salaryId) {
            return salary;
        }

        @Override
        public Salary findByEmployeeAndMonth(long employeeId, int month, int year) {
            if (salary != null && salary.employeeId == employeeId
                    && salary.month == month && salary.year == year) {
                return salary;
            }
            return null;
        }

        @Override
        public List<Salary> findByMonth(int month, int year) {
            List<Salary> result = new ArrayList<>();
            if (salary != null && salary.month == month && salary.year == year) {
                result.add(salary);
            }
            return result;
        }

        @Override
        public List<Salary> findByEmployeeId(long employeeId) {
            List<Salary> result = new ArrayList<>();
            if (salary != null && salary.employeeId == employeeId) {
                result.add(salary);
            }
            return result;
        }

        @Override
        public long countByEmployeeId(long employeeId) {
            return salary != null && salary.employeeId == employeeId ? 1 : 0;
        }
    }

    @Test
    public void checkInCreatesOpenWorkLog() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        WorkLog log = service.checkIn();

        assertNotNull(log);
        assertEquals(7L, log.employeeId);
        assertEquals("2026-10-01", log.workDate);
        assertEquals(Constants.STATUS_OPEN, log.status);
        assertEquals(1, dao.logs.size());
    }

    @Test
    public void checkInTwiceSameDayIsRejected() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        service.checkIn();
        assertThrows(AttendanceException.class, service::checkIn);
        assertEquals(1, dao.logs.size());
    }

    @Test
    public void openPreviousDayBlocksNewCheckIn() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        WorkLog old = new WorkLog(7L, "2026-09-30", "08:00:00", null, 0, Constants.STATUS_OPEN);
        dao.logs.add(old);
        old.id = 99;

        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        assertThrows(AttendanceException.class, service::checkIn);
    }

    @Test
    public void checkOutRequiresCheckIn() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        assertThrows(AttendanceException.class, service::checkOut);
    }

    @Test
    public void checkOutSameInstantIsRejected() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        service.checkIn();
        assertThrows(AttendanceException.class, service::checkOut);
    }

    @Test
    public void checkOutCompletesWorkLog() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        Clock checkInClock = fixedClock("2026-10-01T01:00:00Z");
        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, checkInClock);

        service.checkIn();

        // Đổi đồng hồ sang 8 giờ sau để check-out hợp lệ.
        service.setClock(fixedClock("2026-10-01T09:00:00Z"));
        WorkLog completed = service.checkOut();

        assertEquals(Constants.STATUS_COMPLETED, completed.status);
        assertEquals(8.0, completed.totalHours, 0.0001);
        assertNotNull(completed.checkOutTime);
    }

    @Test
    public void secondCheckOutIsRejected() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));
        service.checkIn();
        service.setClock(fixedClock("2026-10-01T09:00:00Z"));
        service.checkOut();

        assertThrows(AttendanceException.class, service::checkOut);
    }

    @Test
    public void paidMonthBlocksCheckIn() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        FakeSalaryDao salaryDao = new FakeSalaryDao();
        Salary paid = new Salary(7L, 10, 2026, 8, 0, 1000L, Constants.STATUS_PAID);
        salaryDao.salary = paid;
        AttendanceService service = new AttendanceService(
                dao, salaryDao, () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        assertThrows(AttendanceException.class, service::checkIn);
    }

    @Test
    public void historyAndCompletedQueriesUseEmployee() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        WorkLog completed = new WorkLog(7L, "2026-10-02", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED);
        completed.id = 1;
        dao.logs.add(completed);
        WorkLog open = new WorkLog(7L, "2026-10-03", "08:00:00", null, 0, Constants.STATUS_OPEN);
        open.id = 2;
        dao.logs.add(open);

        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-03T01:00:00Z"));

        assertEquals(2, service.getAttendanceHistory(10, 2026).size());
        assertEquals(1, service.getCompletedLogsByMonth(7L, 10, 2026).size());
    }

    @Test
    public void calculateWorkMinutesDelegatesToCalculator() {
        AttendanceService service = new AttendanceService(new FakeWorkLogDao());
        assertEquals(0, service.calculateWorkMinutes(
                java.time.LocalTime.of(8, 0, 10), java.time.LocalTime.of(8, 1, 5)));
    }

    @Test
    public void paidMonthBlocksCheckOut() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        FakeSalaryDao salaryDao = new FakeSalaryDao();
        AttendanceService service = new AttendanceService(
                dao, salaryDao, () -> 7L, fixedClock("2026-10-01T01:00:00Z"));
        service.checkIn();

        salaryDao.salary = new Salary(7L, 10, 2026, 8, 0, 1000L, Constants.STATUS_PAID);
        service.setClock(fixedClock("2026-10-01T09:00:00Z"));

        assertThrows(AttendanceException.class, service::checkOut);
    }

    @Test
    public void crossDayCheckOutIsRejected() {
        FakeWorkLogDao dao = new FakeWorkLogDao();
        WorkLog old = new WorkLog(7L, "2026-09-30", "08:00:00", null, 0, Constants.STATUS_OPEN);
        old.id = 99;
        dao.logs.add(old);

        AttendanceService service = new AttendanceService(
                dao, new FakeSalaryDao(), () -> 7L, fixedClock("2026-10-01T01:00:00Z"));

        assertThrows(AttendanceException.class, service::checkOut);
    }

    @Test
    public void noSessionIsRejected() {
        AttendanceService service = new AttendanceService(new FakeWorkLogDao());
        assertThrows(AttendanceException.class, service::checkIn);
    }
}
