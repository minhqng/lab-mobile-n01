package com.example.employeemanager.integration;

import static org.junit.Assert.*;
import android.content.Context;
import android.content.SharedPreferences;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.employeemanager.auth.*;
import com.example.employeemanager.attendance.*;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.DataInitializer;
import com.example.employeemanager.data.dto.*;
import com.example.employeemanager.data.entities.*;
import com.example.employeemanager.employee.*;
import com.example.employeemanager.reports.ReportService;
import com.example.employeemanager.salary.*;
import com.example.employeemanager.util.Constants;
import org.junit.*;
import org.junit.runner.RunWith;
import java.time.*;
import java.util.concurrent.*;

/** Nghiệm thu xuyên module với Room thật, PBKDF2 thật và một nguồn phiên. */
@RunWith(AndroidJUnit4.class)
public class IntegratedWorkflowTest {
    private static final String PASSWORD = "KiemThu@2026!";
    private static String hash;
    private AppDB db;
    private SharedPreferences preferences;
    private SessionManager sessions;
    private EmployeeService employees;
    private SalaryService salaries;
    private ReportService reports;
    private long employeeId, employeeAccountId;
    private final Clock october = clock("2026-10-24T05:00:00Z");
    @BeforeClass public static void prepareHash() { hash = PasswordHasher.hashPassword(PASSWORD); }
    @Before public void setup() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDB.class).build();
        preferences = context.getSharedPreferences("integrated_workflow_test", Context.MODE_PRIVATE);
        preferences.edit().clear().commit();
        db.accountDao().insert(new Account("integration-admin", hash, Constants.ROLE_ADMIN));
        employeeAccountId = db.accountDao().insert(new Account("integration-employee", hash, Constants.ROLE_EMPLOYEE));
        employeeId = db.employeeDao().insert(new Employee("TICH-HOP-1", "Nhân Viên Mẫu", "integration@example.com", "0912345678", 50000, employeeAccountId));
        sessions = new SessionManager(preferences, new AuthService(db.accountDao(), db.employeeDao()), db.accountDao(), db.employeeDao());
        employees = new EmployeeService(db, new EmployeeAuthAdapter(sessions));
        salaries = new SalaryService(db, () -> sessions.requireRole(Constants.ROLE_ADMIN), new SalaryCalculator(), october);
        reports = new ReportService(db, sessions);
    }
    @After public void cleanup() { sessions.logout(); db.close(); }
    private static Clock clock(String instant) { return Clock.fixed(Instant.parse(instant), ZoneId.of(Constants.TIME_ZONE_ID)); }
    private void loginAdmin() { assertTrue(sessions.login("integration-admin", PASSWORD).isSuccess()); }
    private void loginEmployee() { assertTrue(sessions.login("integration-employee", PASSWORD).isSuccess()); }
    private AttendanceService attendance(String instant) { return new AttendanceService(db, new AuthSessionProvider(sessions), clock(instant)); }
    private void standardMonth() {
        for (int day = 1; day <= 22; day++) {
            db.workLogDao().insert(new WorkLog(employeeId, String.format(java.util.Locale.ROOT, "2026-09-%02d", day),
                    "08:00:00", day <= 20 ? "16:00:00" : "18:00:00", 0, Constants.STATUS_COMPLETED));
        }
    }

    @Test public void realLoginCreateAttendancePayrollAndPersonalReport() {
        loginAdmin();
        Employee created = employees.createEmployee(new CreateEmployeeRequest("TICH-HOP-2", "Trần Bình", "binh@example.com", "0987654321", "50000", "binh", PASSWORD));
        Account account = db.accountDao().findById(created.accountId);
        assertNotEquals(PASSWORD, account.password);
        assertTrue(PasswordHasher.verifyPassword(PASSWORD, account.password));
        sessions.logout();
        assertTrue(sessions.login("binh", PASSWORD).isSuccess());
        AttendanceService attendance = attendance("2026-09-01T01:00:00Z");
        WorkLog log = attendance.checkIn();
        assertEquals(created.id, log.employeeId);
        assertThrows(AttendanceException.class, attendance::checkIn);
        attendance.setClock(clock("2026-09-01T09:00:00Z"));
        WorkLog completed = attendance.checkOut();
        assertEquals(Constants.STATUS_COMPLETED, completed.status);
        assertEquals(8.0, db.workLogDao().findById(completed.id).totalHours, 0);
        assertEquals(480, reports.getMyMonthlySummary(9, 2026).totalMinutes);
        assertNull(reports.getMySalary(9, 2026));
        loginAdmin();
        Salary draft = salaries.generateMonthlySalary(created.id, 9, 2026);
        assertEquals(draft.id, salaries.generateMonthlySalary(created.id, 9, 2026).id);
        assertEquals(Constants.STATUS_PAID, salaries.confirmSalaryPaid(draft.id).status);
        assertTrue(sessions.login("binh", PASSWORD).isSuccess());
        assertEquals(400000, reports.getMySalary(9, 2026).totalSalary);
        sessions.logout();
        assertThrows(SecurityException.class, attendance::checkIn);
    }

    @Test public void standardSalaryAndReportsUseActualMinutesAndLockPaid() {
        standardMonth(); loginAdmin();
        CompanySummary before = reports.getAdminMonthlySummary(9, 2026);
        assertEquals(10800, before.totalMinutes);
        assertEquals(0, before.getCalculatedSalary());
        Salary salary = salaries.generateMonthlySalary(employeeId, 9, 2026);
        assertEquals(180, salary.totalHours, 0);
        assertEquals(4, salary.overtimeHours, 0);
        assertEquals(9100000, salary.totalSalary);
        assertEquals(salary.id, salaries.generateMonthlySalary(employeeId, 9, 2026).id);
        assertEquals(1, db.salaryDao().countByEmployeeId(employeeId));
        assertEquals(9100000, reports.getAdminMonthlySummary(9, 2026).draftSalary);
        salaries.confirmSalaryPaid(salary.id);
        assertThrows(IllegalStateException.class, () -> salaries.generateMonthlySalary(employeeId, 9, 2026));
        assertThrows(IllegalStateException.class, () -> salaries.confirmSalaryPaid(salary.id));
        assertEquals(9100000, reports.getAdminMonthlySummary(9, 2026).paidSalary);
        loginEmployee();
        MonthlySummary summary = reports.getMyMonthlySummary(9, 2026);
        assertEquals(22, summary.completedDays);
        assertEquals(10800, summary.totalMinutes);
        assertEquals(240, summary.overtimeMinutes);
        assertThrows(AttendanceException.class, () -> attendance("2026-09-28T01:00:00Z").checkIn());
        assertEquals(9100000, reports.getMySalary(9, 2026).totalSalary);
    }

    @Test public void sessionRestorationLogoutRevocationAndRoleBoundaries() {
        assertFalse(sessions.login("integration-admin", "sai").isSuccess());
        loginEmployee();
        SessionManager restored = new SessionManager(preferences, new AuthService(db.accountDao(), db.employeeDao()), db.accountDao(), db.employeeDao());
        assertEquals(employeeId, restored.requireCurrentEmployeeId());
        assertThrows(SecurityException.class, () -> reports.getAdminMonthlySummary(9, 2026));
        assertThrows(SecurityException.class, () -> salaries.getSalariesByMonth(9, 2026));
        assertThrows(SecurityException.class, () -> salaries.getSalaryByMonth(employeeId, 9, 2026));
        assertThrows(EmployeeException.class, () -> employees.searchEmployees(""));
        Account account = db.accountDao().findById(employeeAccountId);
        account.role = Constants.ROLE_ADMIN; db.accountDao().update(account);
        assertNull(restored.getValidatedSession());
        loginAdmin();
        assertThrows(SecurityException.class, () -> attendance("2026-10-24T01:00:00Z").checkIn());
        sessions.logout();
        assertNull(restored.getValidatedSession());
    }

    @Test public void personalReportsCannotChooseAnotherEmployeeOrCreateSalary() {
        standardMonth(); loginAdmin();
        salaries.generateMonthlySalary(employeeId, 9, 2026);
        Employee other = employees.createEmployee(new CreateEmployeeRequest("TICH-HOP-3", "Lê An", "le@example.com", "0987654321", "50000", "le-an", PASSWORD));
        assertTrue(sessions.login("le-an", PASSWORD).isSuccess());
        assertNull(reports.getMySalary(9, 2026));
        assertEquals(0, reports.getMyMonthlySummary(9, 2026).totalMinutes);
        assertEquals(0, db.salaryDao().countByEmployeeId(other.id));
        assertThrows(SecurityException.class, () -> attendance("2026-10-24T01:00:00Z").getCompletedLogsByMonth(employeeId, 9, 2026));
        assertThrows(EmployeeException.class, () -> employees.getEmployeeById(employeeId));
    }

    @Test public void openLogsBlockGenerationAndPaymentAndCurrentMonthCannotBePaid() {
        loginAdmin();
        Salary draft = salaries.generateMonthlySalary(employeeId, 9, 2026);
        db.workLogDao().insert(new WorkLog(employeeId, "2026-09-01", "08:00:00", null, 0, Constants.STATUS_OPEN));
        assertThrows(IllegalStateException.class, () -> salaries.generateMonthlySalary(employeeId, 9, 2026));
        assertThrows(IllegalStateException.class, () -> salaries.confirmSalaryPaid(draft.id));
        Salary current = salaries.generateMonthlySalary(employeeId, 10, 2026);
        assertEquals(0, current.totalSalary);
        assertThrows(IllegalStateException.class, () -> salaries.confirmSalaryPaid(current.id));
        loginEmployee();
        assertThrows(AttendanceException.class, () -> attendance("2026-10-24T01:00:00Z").checkIn());
        assertThrows(AttendanceException.class, () -> attendance("2026-10-24T09:00:00Z").checkOut());
    }

    @Test public void duplicateRequestsAreSerializedByRoom() throws Exception {
        loginEmployee();
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<Boolean> attempt = () -> {
                try { attendance("2026-10-24T01:00:00Z").checkIn(); return true; }
                catch (AttendanceException error) { return false; }
            };
            Future<Boolean> first = pool.submit(attempt), second = pool.submit(attempt);
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, db.workLogDao().countByEmployeeId(employeeId));
        } finally { pool.shutdownNow(); }
    }

    @Test public void duLieuHongKhongDuocBienThanhBangLuongHopLe() {
        loginAdmin();
        WorkLog broken = new WorkLog(employeeId, "2026-09-01", "08:00:00", "16:00:00", 8, "INVALID");
        broken.id = db.workLogDao().insert(broken);
        assertThrows(IllegalArgumentException.class, () -> salaries.generateMonthlySalary(employeeId, 9, 2026));
        assertEquals(0, db.salaryDao().countByEmployeeId(employeeId));
        db.workLogDao().delete(broken);
        Salary salary = new Salary(employeeId, 9, 2026, 0, 0, 0, "INVALID");
        salary.id = db.salaryDao().insert(salary);
        assertThrows(IllegalStateException.class, () -> salaries.generateMonthlySalary(employeeId, 9, 2026));
        assertEquals("INVALID", db.salaryDao().findById(salary.id).status);
    }

    @Test public void seedDoesNotResurrectDeletedEmployeeAndPersistsHashedPasswords() {
        db.clearAllTables();
        DataInitializer.initializeInitialData(db);
        DataInitializer.initializeInitialData(db);
        assertEquals(2, db.accountDao().count());
        assertTrue(sessions.login(DataInitializer.DEMO_ADMIN_USERNAME, DataInitializer.DEMO_ADMIN_RAW_PASSWORD).isSuccess());
        Employee demo = db.employeeDao().findByCode("NV001");
        employees.deleteEmployee(demo.id);
        DataInitializer.initializeInitialData(db);
        assertEquals(0, db.employeeDao().count());
        assertNull(db.accountDao().findByUsername("nv001"));
    }
}
