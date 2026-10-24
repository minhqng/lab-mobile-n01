package com.example.employeemanager.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;

import androidx.room.Room;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;
import com.example.employeemanager.util.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

/**
 * Kiểm thử cơ sở dữ liệu Room trên thiết bị hoặc trình giả lập.
 *
 * <p>Bài kiểm thử xác minh bốn bảng nghiệp vụ thực sự được tạo, các chỉ mục UNIQUE và
 * khóa ngoại RESTRICT hoạt động ở mức SQLite. Dữ liệu dùng cơ sở dữ liệu trong bộ nhớ
 * nên không ảnh hưởng dữ liệu thật của ứng dụng.</p>
 */
@RunWith(AndroidJUnit4.class)
public class AppDatabaseTest {

    private AppDB database;

    @Before
    public void createDatabase() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDB.class).build();
    }

    @After
    public void closeDatabase() {
        database.close();
    }

    @Test
    public void createsExactlyFourBusinessTables() {
        Set<String> businessTables = new HashSet<>();
        SupportSQLiteDatabase sqliteDatabase = database.getOpenHelper().getWritableDatabase();
        try (Cursor cursor = sqliteDatabase.query(
                "SELECT name FROM sqlite_master WHERE type = 'table'")) {
            while (cursor.moveToNext()) {
                String tableName = cursor.getString(0);
                if (!tableName.startsWith("sqlite_")
                        && !"room_master_table".equals(tableName)
                        && !"android_metadata".equals(tableName)) {
                    businessTables.add(tableName);
                }
            }
        }

        Set<String> expected = new HashSet<>();
        expected.add("Account");
        expected.add("Employee");
        expected.add("WorkLog");
        expected.add("Salary");
        assertEquals("Cơ sở dữ liệu phải có đúng bốn bảng nghiệp vụ", expected, businessTables);
    }

    @Test
    public void accountUsernameMustBeUnique() {
        long firstId = database.accountDao().insert(
                new Account("admin", "hash-1", Constants.ROLE_ADMIN));
        assertTrue("Tài khoản đầu tiên phải được tạo", firstId > 0);

        assertThrows(SQLiteConstraintException.class, () -> database.accountDao().insert(
                new Account("admin", "hash-2", Constants.ROLE_ADMIN)));
    }

    @Test
    public void employeeUniqueFieldsAndForeignKeyAreEnforced() {
        long accountFirst = insertAccount("nv001", Constants.ROLE_EMPLOYEE);
        long accountSecond = insertAccount("nv002", Constants.ROLE_EMPLOYEE);

        Employee employee = new Employee(
                "NV001", "Nguyễn Văn An", "an.nv001@example.com",
                "0912345678", 50000L, accountFirst);
        long employeeId = database.employeeDao().insert(employee);
        assertTrue("Nhân viên phải được tạo", employeeId > 0);

        assertThrows(SQLiteConstraintException.class, () -> database.employeeDao().insert(
                new Employee("NV001", "Nguyễn Văn Bình", "binh.nv002@example.com",
                        "0912345679", 50000L, accountSecond)));

        assertThrows(SQLiteConstraintException.class, () -> database.employeeDao().insert(
                new Employee("NV002", "Nguyễn Văn Bình", "an.nv001@example.com",
                        "0912345679", 50000L, accountSecond)));

        assertThrows(SQLiteConstraintException.class, () -> database.employeeDao().insert(
                new Employee("NV003", "Nguyễn Văn Bình", "binh.nv003@example.com",
                        "0912345679", 50000L, accountFirst)));

        Account firstAccount = database.accountDao().findById(accountFirst);
        assertThrows("Không được xóa tài khoản còn nhân viên liên kết",
                SQLiteConstraintException.class,
                () -> database.accountDao().delete(firstAccount));
    }

    @Test
    public void workLogHasOneRecordPerEmployeePerDay() {
        long employeeId = insertEmployee("nv001", "NV001", "an.nv001@example.com");

        long workLogId = database.workLogDao().insert(new WorkLog(
                employeeId, "2026-10-01", "08:00:00", null, 0, Constants.STATUS_OPEN));
        assertTrue("Bản ghi chấm công phải được tạo", workLogId > 0);

        assertThrows(SQLiteConstraintException.class, () -> database.workLogDao().insert(
                new WorkLog(employeeId, "2026-10-01", "09:00:00", null, 0, Constants.STATUS_OPEN)));

        WorkLog saved = database.workLogDao().findByEmployeeAndDate(employeeId, "2026-10-01");
        assertNull("Phiên OPEN phải chưa có giờ ra", saved.checkOutTime);
        assertEquals(Constants.STATUS_OPEN, saved.status);

        assertThrows(SQLiteConstraintException.class, () -> database.workLogDao().insert(
                new WorkLog(999_999L, "2026-10-02", "08:00:00", null, 0, Constants.STATUS_OPEN)));
    }

    @Test
    public void salaryHasOneRecordPerEmployeePerMonth() {
        long employeeId = insertEmployee("nv001", "NV001", "an.nv001@example.com");

        long salaryId = database.salaryDao().insert(new Salary(
                employeeId, 9, 2026, 180.0, 4.0, 9_100_000L, Constants.STATUS_DRAFT));
        assertTrue("Bảng lương phải được tạo", salaryId > 0);

        assertThrows(SQLiteConstraintException.class, () -> database.salaryDao().insert(
                new Salary(employeeId, 9, 2026, 160.0, 0.0, 8_000_000L, Constants.STATUS_DRAFT)));

        Salary saved = database.salaryDao().findByEmployeeAndMonth(employeeId, 9, 2026);
        assertEquals(180.0, saved.totalHours, 0.0001);
        assertEquals(4.0, saved.overtimeHours, 0.0001);
        assertEquals(9_100_000L, saved.totalSalary);
        assertEquals(Constants.STATUS_DRAFT, saved.status);
    }

    @Test
    public void deletingEmployeeWithHistoryIsRejected() {
        long employeeId = insertEmployee("nv001", "NV001", "an.nv001@example.com");
        database.workLogDao().insert(new WorkLog(
                employeeId, "2026-10-01", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED));

        Employee employee = database.employeeDao().findById(employeeId);
        assertThrows("Không được xóa nhân viên đã có lịch sử công",
                SQLiteConstraintException.class,
                () -> database.employeeDao().delete(employee));
    }

    @Test
    public void openWorkLogIsNotCountedInCompletedQueries() {
        long employeeId = insertEmployee("nv001", "NV001", "an.nv001@example.com");
        database.workLogDao().insert(new WorkLog(
                employeeId, "2026-10-01", "08:00:00", null, 0, Constants.STATUS_OPEN));
        database.workLogDao().insert(new WorkLog(
                employeeId, "2026-10-02", "08:00:00", "16:00:00", 8.0, Constants.STATUS_COMPLETED));

        assertEquals(1, database.workLogDao()
                .findCompletedByEmployeeAndMonth(employeeId, "2026-10")
                .size());
        assertEquals(2, database.workLogDao()
                .findByEmployeeAndMonth(employeeId, "2026-10")
                .size());
        assertEquals(8.0, database.reportDao()
                .sumCompletedHoursByEmployeeAndMonth(employeeId, "2026-10"), 0.0001);
    }

    private long insertAccount(String username, String role) {
        return database.accountDao().insert(new Account(username, "hash-" + username, role));
    }

    private long insertEmployee(String username, String code, String email) {
        long accountId = insertAccount(username, Constants.ROLE_EMPLOYEE);
        return database.employeeDao().insert(new Employee(
                code, "Nguyễn Văn An", email, "0912345678", 50000L, accountId));
    }
}
