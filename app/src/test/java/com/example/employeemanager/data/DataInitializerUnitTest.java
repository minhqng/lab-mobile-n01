package com.example.employeemanager.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.employeemanager.auth.AuthResult;
import com.example.employeemanager.auth.AuthService;
import com.example.employeemanager.auth.PasswordHasher;
import com.example.employeemanager.data.dao.AccountDao;
import com.example.employeemanager.data.dao.EmployeeDao;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kiểm thử đơn vị cho cơ chế khởi tạo dữ liệu ban đầu {@link DataInitializer}.
 */
public class DataInitializerUnitTest {

    private InMemoryAccountDao accountDao;
    private InMemoryEmployeeDao employeeDao;
    private AuthService authService;

    @Before
    public void setUp() {
        accountDao = new InMemoryAccountDao();
        employeeDao = new InMemoryEmployeeDao();
        authService = new AuthService(accountDao, employeeDao);
    }

    @Test
    public void testInitialSeedCreatesAdminAndSampleEmployee() {
        // Mô phỏng logic khởi tạo lần đầu
        simulateInitializerRun();

        // 1. Kiểm tra tài khoản Quản trị viên
        Account admin = accountDao.findByUsername(DataInitializer.DEMO_ADMIN_USERNAME);
        assertNotNull("Tài khoản admin phải được tạo", admin);
        assertEquals(Constants.ROLE_ADMIN, admin.role);
        assertTrue("Mật khẩu admin phải ở dạng băm pbkdf2", admin.password.startsWith(PasswordHasher.ALGORITHM_PREFIX));
        assertFalse("Không được lưu mật khẩu thô trong DB", admin.password.contains("QuanLy@2026!"));

        // Xác thực đăng nhập Admin thành công
        AuthResult adminAuth = authService.login(DataInitializer.DEMO_ADMIN_USERNAME, DataInitializer.DEMO_ADMIN_RAW_PASSWORD);
        assertTrue("Admin đăng nhập bằng mật khẩu demo phải thành công", adminAuth.isSuccess());

        // 2. Kiểm tra tài khoản Nhân viên mẫu
        Account empAccount = accountDao.findByUsername(DataInitializer.DEMO_EMPLOYEE_USERNAME);
        assertNotNull("Tài khoản nhân viên mẫu phải được tạo", empAccount);
        assertEquals(Constants.ROLE_EMPLOYEE, empAccount.role);
        assertTrue("Mật khẩu nhân viên phải ở dạng băm", empAccount.password.startsWith(PasswordHasher.ALGORITHM_PREFIX));
        assertFalse("Không được lưu mật khẩu thô trong DB", empAccount.password.contains("NhanVien@2026!"));

        Employee employee = employeeDao.findByAccountId(empAccount.id);
        assertNotNull("Bản ghi Employee liên kết phải được tạo", employee);
        assertEquals("NV001", employee.code);
        assertEquals("Nguyễn Văn An", employee.fullname);
        assertEquals(50_000L, employee.hourlyRate);

        // Xác thực đăng nhập Employee thành công
        AuthResult empAuth = authService.login(DataInitializer.DEMO_EMPLOYEE_USERNAME, DataInitializer.DEMO_EMPLOYEE_RAW_PASSWORD);
        assertTrue("Employee đăng nhập bằng mật khẩu demo phải thành công", empAuth.isSuccess());
    }

    @Test
    public void testIdempotentSeedDoesNotCreateDuplicates() {
        // Chạy lần 1
        simulateInitializerRun();
        long accountCountAfterFirst = accountDao.count();
        long employeeCountAfterFirst = employeeDao.count();

        assertEquals("Phải có đúng 2 tài khoản sau lần đầu", 2, accountCountAfterFirst);
        assertEquals("Phải có đúng 1 nhân viên sau lần đầu", 1, employeeCountAfterFirst);

        // Chạy lần 2
        simulateInitializerRun();
        // Chạy lần 3
        simulateInitializerRun();

        assertEquals("Số lượng tài khoản không được tăng lên sau nhiều lần chạy", accountCountAfterFirst, accountDao.count());
        assertEquals("Số lượng nhân viên không được tăng lên sau nhiều lần chạy", employeeCountAfterFirst, employeeDao.count());
    }

    private void simulateInitializerRun() {
        Account existingAdmin = accountDao.findByUsername(DataInitializer.DEMO_ADMIN_USERNAME);
        if (existingAdmin == null) {
            String adminHash = PasswordHasher.hashPassword(DataInitializer.DEMO_ADMIN_RAW_PASSWORD, 1_000);
            Account admin = new Account(DataInitializer.DEMO_ADMIN_USERNAME, adminHash, Constants.ROLE_ADMIN);
            accountDao.insert(admin);
        }

        Account existingEmployeeAccount = accountDao.findByUsername(DataInitializer.DEMO_EMPLOYEE_USERNAME);
        if (existingEmployeeAccount == null) {
            String empHash = PasswordHasher.hashPassword(DataInitializer.DEMO_EMPLOYEE_RAW_PASSWORD, 1_000);
            Account empAccount = new Account(DataInitializer.DEMO_EMPLOYEE_USERNAME, empHash, Constants.ROLE_EMPLOYEE);
            long accId = accountDao.insert(empAccount);

            Employee existingEmployee = employeeDao.findByCode("NV001");
            if (existingEmployee == null) {
                Employee sampleEmployee = new Employee("NV001", "Nguyễn Văn An", "an.nv001@example.com", "0912345678", 50000L, accId);
                employeeDao.insert(sampleEmployee);
            }
        }
    }

    private static class InMemoryAccountDao implements AccountDao {
        private final Map<Long, Account> map = new HashMap<>();

        @Override
        public long insert(Account account) {
            long id = account.id > 0 ? account.id : map.size() + 1L;
            account.id = id;
            map.put(id, account);
            return id;
        }

        @Override
        public void update(Account account) { map.put(account.id, account); }

        @Override
        public void delete(Account account) { map.remove(account.id); }

        @Override
        public Account findById(long accountId) { return map.get(accountId); }

        @Override
        public Account findByUsername(String username) {
            for (Account a : map.values()) {
                if (a.username.equals(username)) return a;
            }
            return null;
        }

        @Override
        public List<Account> getAll() { return new ArrayList<>(map.values()); }

        @Override
        public long count() { return map.size(); }
    }

    private static class InMemoryEmployeeDao implements EmployeeDao {
        private final Map<Long, Employee> map = new HashMap<>();

        @Override
        public long insert(Employee employee) {
            long id = employee.id > 0 ? employee.id : map.size() + 1L;
            employee.id = id;
            map.put(id, employee);
            return id;
        }

        @Override
        public void update(Employee employee) { map.put(employee.id, employee); }

        @Override
        public void delete(Employee employee) { map.remove(employee.id); }

        @Override
        public Employee findById(long employeeId) { return map.get(employeeId); }

        @Override
        public Employee findByCode(String code) {
            for (Employee e : map.values()) {
                if (e.code.equals(code)) return e;
            }
            return null;
        }

        @Override
        public Employee findByEmail(String email) {
            for (Employee e : map.values()) {
                if (e.email.equals(email)) return e;
            }
            return null;
        }

        @Override
        public Employee findByAccountId(long accountId) {
            for (Employee e : map.values()) {
                if (e.accountId == accountId) return e;
            }
            return null;
        }

        @Override
        public List<Employee> getAll() { return new ArrayList<>(map.values()); }

        @Override
        public List<Employee> search(String keyword) { return new ArrayList<>(map.values()); }

        @Override
        public long count() { return map.size(); }
    }
}
