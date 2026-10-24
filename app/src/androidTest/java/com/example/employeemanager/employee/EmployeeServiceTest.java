package com.example.employeemanager.employee;

import static org.junit.Assert.*;
import android.database.sqlite.SQLiteConstraintException;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.*;
import com.example.employeemanager.util.Constants;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.concurrent.*;

/** Giao dịch được kiểm tra bằng Room thật; bộ băm giả chỉ tồn tại trong kiểm thử. */
@RunWith(AndroidJUnit4.class)
public class EmployeeServiceTest {
    private AppDB database;
    private EmployeeService service;
    private long currentId;
    private String hash = "gia-tri-bam-chi-dung-trong-kiem-thu";

    @Before public void setup() {
        database = Room.inMemoryDatabaseBuilder(
                InstrumentationRegistry.getInstrumentation().getTargetContext(), AppDB.class).build();
        currentId = database.accountDao().insert(new Account("admin-test", "bam-kiem-thu", Constants.ROLE_ADMIN));
        service = new EmployeeService(database, new EmployeeAuth() {
            @Override public long currentAccountId() { return currentId; }
            @Override public String hashPassword(String plainPassword) { return hash; }
        });
    }

    @After public void cleanup() { database.close(); }

    private CreateEmployeeRequest request(String code, String email, String username) {
        return new CreateEmployeeRequest(code, "Nguyễn Ánh", email, "0912345678", "50000", username, "đầu vào kiểm thử");
    }
    private Employee create() { return service.createEmployee(request("NV001", "anh@example.com", "nv001")); }
    private UpdateEmployeeRequest update(String code, String email) {
        return new UpdateEmployeeRequest(code, "Trần Bình", email, "0987654321", "75000");
    }
    private void expectError(int resource, Runnable action) {
        assertEquals(resource, assertThrows(EmployeeException.class, action::run).messageRes);
    }

    @Test public void createsNormalizedEmployeeAndEmployeeAccount() {
        Employee employee = service.createEmployee(request(" nv001 ", " ANH@EXAMPLE.COM ", " NV001 "));
        Account account = database.accountDao().findById(employee.accountId);
        assertEquals("nv001", account.username);
        assertEquals(Constants.ROLE_EMPLOYEE, account.role);
        assertEquals(hash, account.password);
        assertNotEquals("đầu vào kiểm thử", account.password);
        assertEquals("NV001", database.employeeDao().findById(employee.id).code);
        assertEquals("anh@example.com", employee.email);
        assertEquals(2, database.accountDao().count());
        assertEquals(1, database.employeeDao().count());
    }
    @Test public void duplicateCodeDoesNotCreateOrphan() {
        create();
        expectError(R.string.nv_trung_ma, () -> service.createEmployee(request(" nv001 ", "other@example.com", "other")));
        assertEquals(2, database.accountDao().count());
    }
    @Test public void duplicateEmailDoesNotCreateOrphan() {
        create();
        expectError(R.string.nv_trung_email, () -> service.createEmployee(request("NV002", " ANH@EXAMPLE.COM ", "other")));
        assertEquals(2, database.accountDao().count());
    }
    @Test public void duplicateUsernameDoesNotCreateEmployee() {
        create();
        expectError(R.string.nv_trung_tai_khoan, () -> service.createEmployee(request("NV002", "other@example.com", " NV001 ")));
        assertEquals(1, database.employeeDao().count());
    }
    @Test public void employeeInsertFailureRollsBackAccount() {
        database.getOpenHelper().getWritableDatabase().execSQL(
                "CREATE TRIGGER reject_employee BEFORE INSERT ON Employee BEGIN SELECT RAISE(ABORT, 'Từ chối tạo để kiểm thử'); END");
        expectError(R.string.nv_loi_luu, this::create);
        assertEquals(1, database.accountDao().count());
        assertEquals(0, database.employeeDao().count());
        assertNull(database.accountDao().findByUsername("nv001"));
    }
    @Test public void absentHasherFailsSafely() {
        hash = null;
        expectError(R.string.nv_auth_chua_san_sang, this::create);
        assertEquals(1, database.accountDao().count());
    }
    @Test public void plaintextHasherFailsSafely() {
        hash = "đầu vào kiểm thử";
        expectError(R.string.nv_auth_chua_san_sang, this::create);
        assertEquals(0, database.employeeDao().count());
    }
    @Test public void updatePreservesIdentityAccountAndCredentials() {
        Employee original = create();
        Employee updated = service.updateEmployee(original.id, update(" nv001 ", " ANH@EXAMPLE.COM "));
        assertEquals(original.id, updated.id);
        assertEquals(original.accountId, updated.accountId);
        Employee saved = database.employeeDao().findById(original.id);
        assertEquals("Trần Bình", saved.fullname);
        assertEquals(75000, saved.hourlyRate);
        Account account = database.accountDao().findById(original.accountId);
        assertEquals("nv001", account.username);
        assertEquals(hash, account.password);
        assertEquals(Constants.ROLE_EMPLOYEE, account.role);
    }
    @Test public void updateRejectsOtherEmployeesCode() {
        Employee original = create();
        service.createEmployee(request("NV002", "other@example.com", "other"));
        expectError(R.string.nv_trung_ma, () -> service.updateEmployee(original.id, update("NV002", original.email)));
        assertEquals("NV001", database.employeeDao().findById(original.id).code);
    }
    @Test public void updateRejectsOtherEmployeesEmail() {
        Employee original = create();
        service.createEmployee(request("NV002", "other@example.com", "other"));
        expectError(R.string.nv_trung_email, () -> service.updateEmployee(original.id, update("NV001", "OTHER@EXAMPLE.COM")));
        assertEquals(original.email, database.employeeDao().findById(original.id).email);
    }
    @Test public void missingIdsFailClearly() {
        expectError(R.string.nv_khong_ton_tai, () -> service.getEmployeeById(999));
        expectError(R.string.nv_khong_ton_tai, () -> service.getEmployeeByAccountId(999));
        expectError(R.string.nv_khong_ton_tai, () -> service.updateEmployee(999, update("NV", "a@b.vn")));
        expectError(R.string.nv_khong_ton_tai, () -> service.deleteEmployee(999));
    }
    @Test public void searchesCodeNameAndEmailIgnoringVietnameseCase() {
        Employee employee = create();
        for (String keyword : new String[]{" nv0 ", "NGUYỄN ÁNH", "ANH@EXAMPLE", "", "  "})
            assertEquals(employee.id, service.searchEmployees(keyword).get(0).id);
    }
    @Test public void treatsWildcardAndSqlAsLiteralText() {
        create();
        assertTrue(service.searchEmployees("%").isEmpty());
        assertTrue(service.searchEmployees("_").isEmpty());
        assertTrue(service.searchEmployees("' OR 1=1 --").isEmpty());
        Employee special = service.createEmployee(request("NV%_", "other@example.com", "other"));
        assertEquals(special.id, service.searchEmployees("%_").get(0).id);
    }
    @Test public void emptyListAndNullKeywordAreSupported() {
        assertTrue(service.searchEmployees(null).isEmpty());
        assertTrue(database.employeeDao().search(" ").isEmpty());
    }
    @Test public void searchOrderIsStable() {
        Employee first = create();
        Employee second = service.createEmployee(request("NV002", "other@example.com", "other"));
        List<Employee> result = service.searchEmployees("");
        assertEquals(first.id, result.get(0).id);
        assertEquals(second.id, result.get(1).id);
    }
    @Test public void deletesEmployeeAndLinkedAccount() {
        Employee employee = create();
        service.deleteEmployee(employee.id);
        assertNull(database.employeeDao().findById(employee.id));
        assertNull(database.accountDao().findById(employee.accountId));
        assertEquals(1, database.accountDao().count());
    }
    @Test public void rejectsDeletionWithWorkLog() {
        Employee employee = create();
        database.workLogDao().insert(new WorkLog(employee.id, "2026-10-01", "08:00:00", null, 0, Constants.STATUS_OPEN));
        expectError(R.string.nv_co_lich_su, () -> service.deleteEmployee(employee.id));
        assertNotNull(database.employeeDao().findById(employee.id));
        assertNotNull(database.accountDao().findById(employee.accountId));
        assertEquals(1, database.workLogDao().countByEmployeeId(employee.id));
    }
    @Test public void rejectsDeletionWithSalary() {
        Employee employee = create();
        database.salaryDao().insert(new Salary(employee.id, 9, 2026, 0, 0, 0, Constants.STATUS_DRAFT));
        expectError(R.string.nv_co_lich_su, () -> service.deleteEmployee(employee.id));
        assertNotNull(database.employeeDao().findById(employee.id));
        assertEquals(1, database.salaryDao().countByEmployeeId(employee.id));
    }
    @Test public void accountDeleteFailureRollsBackEmployeeDelete() {
        Employee employee = create();
        database.getOpenHelper().getWritableDatabase().execSQL(
                "CREATE TRIGGER reject_account_delete BEFORE DELETE ON Account BEGIN SELECT RAISE(ABORT, 'Từ chối xóa để kiểm thử'); END");
        expectError(R.string.nv_loi_xoa, () -> service.deleteEmployee(employee.id));
        assertNotNull(database.employeeDao().findById(employee.id));
        assertNotNull(database.accountDao().findById(employee.accountId));
    }
    @Test public void invalidForeignKeyAndDuplicateAccountAreRejected() {
        Employee employee = create();
        assertThrows(SQLiteConstraintException.class, () -> database.employeeDao().insert(
                new Employee("NV002", "Trần Bình", "other@example.com", "0912345678", 1, 99999)));
        assertThrows(SQLiteConstraintException.class, () -> database.employeeDao().insert(
                new Employee("NV002", "Trần Bình", "other@example.com", "0912345678", 1, employee.accountId)));
    }
    @Test public void employeeCannotInvokeAdminOperationsDirectly() {
        Employee own = create();
        Employee other = service.createEmployee(request("NV002", "other@example.com", "other"));
        currentId = own.accountId;
        expectError(R.string.nv_khong_co_quyen, this::create);
        expectError(R.string.nv_khong_co_quyen, () -> service.updateEmployee(own.id, update("NV", "a@b.vn")));
        expectError(R.string.nv_khong_co_quyen, () -> service.deleteEmployee(own.id));
        expectError(R.string.nv_khong_co_quyen, () -> service.searchEmployees(""));
        expectError(R.string.nv_khong_co_quyen, () -> service.getEmployeeById(other.id));
        expectError(R.string.nv_khong_co_quyen, () -> service.getEmployeeByAccountId(other.accountId));
        assertEquals(own.id, service.getEmployeeById(own.id).id);
        assertEquals(own.id, service.getEmployeeByAccountId(own.accountId).id);
    }
    @Test public void missingSessionIsDenied() {
        currentId = 0;
        expectError(R.string.nv_khong_co_quyen, this::create);
        expectError(R.string.nv_khong_co_quyen, () -> service.searchEmployees(""));
        expectError(R.string.nv_khong_co_quyen, () -> service.getEmployeeById(1));
    }
    @Test public void revalidatesRoleAndAccountFromDatabase() {
        Account current = database.accountDao().findById(currentId);
        current.role = Constants.ROLE_EMPLOYEE;
        database.accountDao().update(current);
        expectError(R.string.nv_khong_co_quyen, () -> service.searchEmployees(""));
        database.accountDao().delete(current);
        expectError(R.string.nv_khong_co_quyen, () -> service.requireAdmin());
    }
    @Test public void serviceCannotBypassValidation() {
        expectError(R.string.nv_loi_don_gia, () -> service.createEmployee(
                new CreateEmployeeRequest("NV", "An", "a@b.vn", "0912345678", "0", "nv", "đầu vào kiểm thử")));
        assertEquals(0, database.employeeDao().count());
    }
    @Test public void concurrentDuplicateCreationKeepsOnePair() throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Boolean> action = () -> {
            start.await();
            try { create(); return true; }
            catch (EmployeeException expected) { return false; }
        };
        try {
            Future<Boolean> first = workers.submit(action), second = workers.submit(action);
            start.countDown();
            assertNotEquals(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertEquals(1, database.employeeDao().count());
            assertEquals(2, database.accountDao().count());
        } finally { workers.shutdownNow(); }
    }
}
