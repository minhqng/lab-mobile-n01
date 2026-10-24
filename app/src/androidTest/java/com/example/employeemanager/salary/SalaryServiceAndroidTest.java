package com.example.employeemanager.salary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Kiểm thử tích hợp cho {@link SalaryService} trên cơ sở dữ liệu Room in-memory.
 */
@RunWith(AndroidJUnit4.class)
public class SalaryServiceAndroidTest {

    private AppDB database;
    private SalaryService salaryService;
    private TestAuthorization adminAuth;
    private TestAuthorization nonAdminAuth;
    private long employeeId;

    // Giả lập thời điểm hiện tại: 2026-10-24 12:00:00 theo múi giờ Việt Nam
    private static final ZoneId ZONE_VN = ZoneId.of(Constants.TIME_ZONE_ID);
    private static final Instant OCT_2026 = Instant.parse("2026-10-24T05:00:00Z");

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDB.class).build();

        adminAuth = new TestAuthorization(true);
        nonAdminAuth = new TestAuthorization(false);

        Clock fixedClock = Clock.fixed(OCT_2026, ZONE_VN);
        salaryService = new SalaryService(database, adminAuth, new SalaryCalculator(), fixedClock);

        // Chuẩn bị nhân viên mẫu
        long accountId = database.accountDao().insert(
                new Account("nv001", "hash-nv001", Constants.ROLE_EMPLOYEE));
        employeeId = database.employeeDao().insert(
                new Employee("NV001", "Nguyễn Văn An", "an@example.com", "0912345678", 50_000L, accountId));
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void testGenerateMonthlySalary_CreatesDraftSuccessfully() {
        // Thêm 2 ngày làm 8 giờ vào tháng 09/2026: 2 * 480 = 960 phút = 16 giờ, 0 OT -> 800.000đ
        database.workLogDao().insert(new WorkLog(employeeId, "2026-09-01", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED));
        database.workLogDao().insert(new WorkLog(employeeId, "2026-09-02", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED));

        Salary salary = salaryService.generateMonthlySalary(employeeId, 9, 2026);

        assertNotNull(salary);
        assertTrue(salary.id > 0);
        assertEquals(employeeId, salary.employeeId);
        assertEquals(9, salary.month);
        assertEquals(2026, salary.year);
        assertEquals(16.0, salary.totalHours, 0.0001);
        assertEquals(0.0, salary.overtimeHours, 0.0001);
        assertEquals(800_000L, salary.totalSalary);
        assertEquals(Constants.STATUS_DRAFT, salary.status);
    }

    @Test
    public void testGenerateMonthlySalary_NoWorkLogs_CreatesDraftZero() {
        // Tháng 08/2026 không có công
        Salary salary = salaryService.generateMonthlySalary(employeeId, 8, 2026);

        assertNotNull(salary);
        assertEquals(0.0, salary.totalHours, 0.0001);
        assertEquals(0.0, salary.overtimeHours, 0.0001);
        assertEquals(0L, salary.totalSalary);
        assertEquals(Constants.STATUS_DRAFT, salary.status);
    }

    @Test
    public void testGenerateMonthlySalary_WithOpenWorkLog_ThrowsException() {
        // Thêm bản ghi OPEN trong tháng 09/2026
        database.workLogDao().insert(new WorkLog(employeeId, "2026-09-05", "08:00:00", null, 0.0, Constants.STATUS_OPEN));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                salaryService.generateMonthlySalary(employeeId, 9, 2026)
        );
        assertTrue(ex.getMessage().contains("chưa hoàn tất"));
    }

    @Test
    public void testGenerateMonthlySalary_RecalculateDraft_UpdatesSameRecord() {
        // Lần 1: 1 ngày 8h -> 400.000đ
        database.workLogDao().insert(new WorkLog(employeeId, "2026-09-01", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED));
        Salary salary1 = salaryService.generateMonthlySalary(employeeId, 9, 2026);
        assertEquals(400_000L, salary1.totalSalary);

        // Bổ sung thêm ngày làm 10h (2h OT)
        database.workLogDao().insert(new WorkLog(employeeId, "2026-09-02", "08:00:00", "18:00:00", 10.0, Constants.STATUS_COMPLETED));

        // Lần 2: tính lại
        Salary salary2 = salaryService.generateMonthlySalary(employeeId, 9, 2026);
        assertEquals(salary1.id, salary2.id); // Cùng ID
        assertEquals(18.0, salary2.totalHours, 0.0001);
        assertEquals(2.0, salary2.overtimeHours, 0.0001);
        assertEquals(950_000L, salary2.totalSalary);

        // Kiểm tra database chỉ có duy nhất 1 bản ghi
        List<Salary> list = database.salaryDao().findByMonth(9, 2026);
        assertEquals(1, list.size());
    }

    @Test
    public void testGenerateMonthlySalary_PaidSalaryCannotBeRecalculated() {
        // Tạo bảng lương PAID
        database.salaryDao().insert(new Salary(employeeId, 8, 2026, 160.0, 0.0, 8_000_000L, Constants.STATUS_PAID));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                salaryService.generateMonthlySalary(employeeId, 8, 2026)
        );
        assertTrue(ex.getMessage().contains("đã thanh toán"));
    }

    @Test
    public void testConfirmSalaryPaid_OnlyEndedMonthsAllowed() {
        // Thời điểm hiện tại là 10/2026. Tháng 09/2026 đã kết thúc -> được chốt.
        Salary salaryPast = salaryService.generateMonthlySalary(employeeId, 9, 2026);
        Salary paidPast = salaryService.confirmSalaryPaid(salaryPast.id);
        assertEquals(Constants.STATUS_PAID, paidPast.status);

        // Tháng 10/2026 là tháng hiện tại -> không được chốt.
        Salary salaryCurrent = salaryService.generateMonthlySalary(employeeId, 10, 2026);
        IllegalStateException exCurrent = assertThrows(IllegalStateException.class, () ->
                salaryService.confirmSalaryPaid(salaryCurrent.id)
        );
        assertTrue(exCurrent.getMessage().contains("tháng đã kết thúc"));

        // Tháng 11/2026 là tháng tương lai -> không được chốt.
        Salary salaryFuture = salaryService.generateMonthlySalary(employeeId, 11, 2026);
        IllegalStateException exFuture = assertThrows(IllegalStateException.class, () ->
                salaryService.confirmSalaryPaid(salaryFuture.id)
        );
        assertTrue(exFuture.getMessage().contains("tháng đã kết thúc"));
    }

    @Test
    public void testConfirmSalaryPaid_WithOpenLog_ThrowsException() {
        // Tạo DRAFT tháng 9 khi chưa có log OPEN
        Salary salary = salaryService.generateMonthlySalary(employeeId, 9, 2026);

        // Sau đó xuất hiện log OPEN trong tháng 9
        database.workLogDao().insert(new WorkLog(employeeId, "2026-09-30", "08:00:00", null, 0.0, Constants.STATUS_OPEN));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                salaryService.confirmSalaryPaid(salary.id)
        );
        assertTrue(ex.getMessage().contains("chưa hoàn tất"));
    }

    @Test
    public void testConfirmSalaryPaid_AlreadyPaid_ThrowsException() {
        Salary salary = salaryService.generateMonthlySalary(employeeId, 9, 2026);
        salaryService.confirmSalaryPaid(salary.id);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                salaryService.confirmSalaryPaid(salary.id)
        );
        assertTrue(ex.getMessage().contains("đã thanh toán"));
    }

    @Test
    public void testAuthorization_DeniedForNonAdmin() {
        SalaryService nonAdminService = new SalaryService(database, nonAdminAuth, new SalaryCalculator(), Clock.fixed(OCT_2026, ZONE_VN));

        assertThrows(SecurityException.class, () ->
                nonAdminService.generateMonthlySalary(employeeId, 9, 2026)
        );

        assertThrows(SecurityException.class, () ->
                nonAdminService.getSalariesByMonth(9, 2026)
        );

        assertThrows(SecurityException.class, () ->
                nonAdminService.confirmSalaryPaid(1L)
        );

        assertThrows(SecurityException.class, () ->
                nonAdminService.getEmployeesForSalaryManagement()
        );

        assertThrows(SecurityException.class, () ->
                nonAdminService.getSalaryDetail(1L)
        );
    }

    @Test
    public void testGetEmployeesForSalaryManagement_AllowedForAdmin() {
        List<Employee> employees = salaryService.getEmployeesForSalaryManagement();
        assertNotNull(employees);
        assertEquals(1, employees.size());
        assertEquals("NV001", employees.get(0).code);
    }

    @Test
    public void testGetSalaryDetail_AllowedForAdmin() {
        Salary salary = salaryService.generateMonthlySalary(employeeId, 9, 2026);
        com.example.employeemanager.data.dto.SalaryDetailDto detail = salaryService.getSalaryDetail(salary.id);

        assertNotNull(detail);
        assertNotNull(detail.getSalary());
        assertNotNull(detail.getEmployee());
        assertEquals(salary.id, detail.getSalary().id);
        assertEquals("NV001", detail.getEmployee().code);
    }

    @Test
    public void testBoundaryClock_YearBoundaryComparison() {
        // Đồng hồ ở ngày 01/01/2027 00:00:01 múi giờ VN (tức 31/12/2026 17:00:01 UTC)
        Instant newYearInstant = Instant.parse("2026-12-31T17:00:01Z");
        Clock yearBoundaryClock = Clock.fixed(newYearInstant, ZONE_VN);
        SalaryService boundaryService = new SalaryService(database, adminAuth, new SalaryCalculator(), yearBoundaryClock);

        // Tháng 12/2026 là tháng trước (đã kết thúc so với tháng 01/2027) -> được chốt!
        Salary salaryDec = boundaryService.generateMonthlySalary(employeeId, 12, 2026);
        Salary confirmed = boundaryService.confirmSalaryPaid(salaryDec.id);
        assertEquals(Constants.STATUS_PAID, confirmed.status);

        // Tháng 01/2027 là tháng hiện tại -> không được chốt!
        Salary salaryJan = boundaryService.generateMonthlySalary(employeeId, 1, 2027);
        assertThrows(IllegalStateException.class, () ->
                boundaryService.confirmSalaryPaid(salaryJan.id)
        );
    }

    private static class TestAuthorization implements SalaryAuthorization {
        private final boolean isAdmin;

        TestAuthorization(boolean isAdmin) {
            this.isAdmin = isAdmin;
        }

        @Override
        public void requireAdmin() {
            if (!isAdmin) {
                throw new SecurityException("Tài khoản không có quyền truy cập");
            }
        }
    }
}
