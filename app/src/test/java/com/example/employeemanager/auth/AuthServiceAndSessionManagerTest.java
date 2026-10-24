package com.example.employeemanager.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.SharedPreferences;

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
import java.util.Set;

/**
 * Kiểm thử đơn vị cho {@link AuthService} và {@link SessionManager}.
 */
public class AuthServiceAndSessionManagerTest {

    private FakeAccountDao fakeAccountDao;
    private FakeEmployeeDao fakeEmployeeDao;
    private FakeSharedPreferences fakeSharedPreferences;
    private AuthService authService;
    private SessionManager sessionManager;

    @Before
    public void setUp() {
        fakeAccountDao = new FakeAccountDao();
        fakeEmployeeDao = new FakeEmployeeDao();
        fakeSharedPreferences = new FakeSharedPreferences();
        authService = new AuthService(fakeAccountDao, fakeEmployeeDao);
        sessionManager = new SessionManager(fakeSharedPreferences, authService, fakeAccountDao, fakeEmployeeDao);

        // Khởi tạo tài khoản Admin và Employee mẫu trong Fake DB
        // Dùng 1.000 vòng băm để kiểm thử chạy cực nhanh
        String adminHash = PasswordHasher.hashPassword("QuanLy@2026!", 1_000);
        Account admin = new Account(1L, "admin", adminHash, Constants.ROLE_ADMIN);
        fakeAccountDao.insert(admin);

        String empHash = PasswordHasher.hashPassword("NhanVien@2026!", 1_000);
        Account empAccount = new Account(2L, "nv001", empHash, Constants.ROLE_EMPLOYEE);
        fakeAccountDao.insert(empAccount);

        Employee employee = new Employee(10L, "NV001", "Nguyễn Văn An", "an.nv001@example.com", "0912345678", 50000L, 2L);
        fakeEmployeeDao.insert(employee);
    }

    @Test
    public void testUsernameNormalization() {
        assertEquals("admin", AuthService.normalizeUsername("  ADMIN  "));
        assertEquals("nv001", AuthService.normalizeUsername("Nv001 "));
        assertEquals("", AuthService.normalizeUsername(null));
        assertEquals("", AuthService.normalizeUsername("   "));
    }

    @Test
    public void testAdminLoginSuccess() {
        AuthResult result = sessionManager.login("  Admin  ", "QuanLy@2026!");

        assertTrue("Đăng nhập Admin phải thành công", result.isSuccess());
        assertEquals("admin", result.getUsername());
        assertEquals(Constants.ROLE_ADMIN, result.getRole());
        assertEquals(1L, result.getAccountId());
        assertNull("Không có thông báo lỗi khi đăng nhập thành công", result.getErrorMessage());

        SessionData session = sessionManager.getValidatedSession();
        assertNotNull("Session phải được lưu sau khi đăng nhập", session);
        assertEquals(1L, session.getAccountId());
        assertEquals(Constants.ROLE_ADMIN, session.getRole());
        assertTrue(session.isAdmin());
        assertFalse(session.isEmployee());
    }

    @Test
    public void testEmployeeLoginSuccess() {
        AuthResult result = sessionManager.login("nv001", "NhanVien@2026!");

        assertTrue("Đăng nhập Employee phải thành công", result.isSuccess());
        assertEquals("nv001", result.getUsername());
        assertEquals(Constants.ROLE_EMPLOYEE, result.getRole());
        assertEquals(2L, result.getAccountId());

        SessionData session = sessionManager.getValidatedSession();
        assertNotNull("Session Employee phải tồn tại", session);
        assertTrue(session.isEmployee());
        assertEquals(Long.valueOf(10L), session.getEmployeeId());
    }

    @Test
    public void testLoginWithWrongPasswordRejected() {
        AuthResult result = sessionManager.login("admin", "MatKhauSai@123");

        assertFalse("Đăng nhập sai mật khẩu phải thất bại", result.isSuccess());
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng.", result.getErrorMessage());
        assertNull("Không được có session khi đăng nhập thất bại", sessionManager.getValidatedSession());
    }

    @Test
    public void testLoginWithNonExistentAccountRejected() {
        AuthResult result = sessionManager.login("khongtontai", "QuanLy@2026!");

        assertFalse("Tài khoản không tồn tại phải thất bại", result.isSuccess());
        assertEquals("Tên đăng nhập hoặc mật khẩu không đúng.", result.getErrorMessage());
    }

    @Test
    public void testLoginWithEmptyCredentialsRejected() {
        AuthResult result1 = sessionManager.login("", "matkhau");
        assertFalse("Tên đăng nhập rỗng phải bị từ chối", result1.isSuccess());
        assertEquals("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", result1.getErrorMessage());

        AuthResult result2 = sessionManager.login("admin", "");
        assertFalse("Mật khẩu rỗng phải bị từ chối", result2.isSuccess());
        assertEquals("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.", result2.getErrorMessage());
    }

    @Test
    public void testRoleRequirementEnforcement() {
        sessionManager.login("admin", "QuanLy@2026!");

        // Admin được chấp nhận cho vai trò ADMIN
        sessionManager.requireRole(Constants.ROLE_ADMIN);

        // Admin không được phép có vai trò EMPLOYEE
        assertThrows(SecurityException.class, () -> sessionManager.requireRole(Constants.ROLE_EMPLOYEE));

        // Admin không được gọi requireCurrentEmployeeId()
        assertThrows(SecurityException.class, () -> sessionManager.requireCurrentEmployeeId());

        // Đổi sang tài khoản Employee
        sessionManager.logout();
        sessionManager.login("nv001", "NhanVien@2026!");

        // Employee được chấp nhận cho EMPLOYEE
        sessionManager.requireRole(Constants.ROLE_EMPLOYEE);
        assertEquals(10L, sessionManager.requireCurrentEmployeeId());

        // Employee không được phép có vai trò ADMIN
        assertThrows(SecurityException.class, () -> sessionManager.requireRole(Constants.ROLE_ADMIN));
    }

    @Test
    public void testSessionInvalidatedWhenAccountDeletedFromDatabase() {
        sessionManager.login("admin", "QuanLy@2026!");
        assertNotNull("Session phải hợp lệ ban đầu", sessionManager.getValidatedSession());

        // Xóa tài khoản admin khỏi database
        Account admin = fakeAccountDao.findById(1L);
        fakeAccountDao.delete(admin);

        // getValidatedSession phải phát hiện tài khoản không còn trong Room và tự động xóa phiên
        SessionData sessionAfterDelete = sessionManager.getValidatedSession();
        assertNull("Session phải bị vô hiệu hóa khi tài khoản bị xóa", sessionAfterDelete);
    }

    @Test
    public void testSessionInvalidatedWhenRoleChangedInDatabase() {
        sessionManager.login("admin", "QuanLy@2026!");
        assertNotNull(sessionManager.getValidatedSession());

        // Giả sử vai trò bị đổi trong DB từ ADMIN sang EMPLOYEE
        Account admin = fakeAccountDao.findById(1L);
        admin.role = Constants.ROLE_EMPLOYEE;
        fakeAccountDao.update(admin);

        // getValidatedSession phải từ chối phiên vì role trong DB không khớp với SharedPreferences
        SessionData sessionAfterRoleChange = sessionManager.getValidatedSession();
        assertNull("Session phải bị vô hiệu hóa khi role trong DB bị thay đổi", sessionAfterRoleChange);
    }

    @Test
    public void testLogoutClearsSession() {
        sessionManager.login("admin", "QuanLy@2026!");
        assertNotNull(sessionManager.getValidatedSession());

        sessionManager.logout();

        assertNull("Sau khi logout, session phải null", sessionManager.getValidatedSession());
        assertFalse("SharedPreferences không còn lưu cờ đăng nhập", fakeSharedPreferences.getBoolean("is_logged_in", false));
    }

    @Test
    public void testNoPlainTextPasswordStoredInPreferences() {
        sessionManager.login("admin", "QuanLy@2026!");

        Map<String, ?> all = fakeSharedPreferences.getAll();
        for (Object value : all.values()) {
            if (value instanceof String) {
                assertFalse("Không được chứa mật khẩu thô trong SharedPreferences", ((String) value).contains("QuanLy@2026!"));
                assertFalse("Không được chứa hash mật khẩu trong SharedPreferences", ((String) value).contains("pbkdf2_sha256"));
            }
        }
    }

    // ==========================================
    // CÁC LỚP FAKE PHỤC VỤ KIỂM THỬ ĐƠN VỊ THUẦN
    // ==========================================

    private static class FakeAccountDao implements AccountDao {
        private final Map<Long, Account> accounts = new HashMap<>();

        @Override
        public long insert(Account account) {
            long id = account.id > 0 ? account.id : accounts.size() + 1L;
            account.id = id;
            accounts.put(id, account);
            return id;
        }

        @Override
        public void update(Account account) {
            accounts.put(account.id, account);
        }

        @Override
        public void delete(Account account) {
            accounts.remove(account.id);
        }

        @Override
        public Account findById(long accountId) {
            return accounts.get(accountId);
        }

        @Override
        public Account findByUsername(String username) {
            for (Account account : accounts.values()) {
                if (account.username.equals(username)) {
                    return account;
                }
            }
            return null;
        }

        @Override
        public List<Account> getAll() {
            return new ArrayList<>(accounts.values());
        }

        @Override
        public long count() {
            return accounts.size();
        }
    }

    private static class FakeEmployeeDao implements EmployeeDao {
        private final Map<Long, Employee> employees = new HashMap<>();

        @Override
        public long insert(Employee employee) {
            long id = employee.id > 0 ? employee.id : employees.size() + 1L;
            employee.id = id;
            employees.put(id, employee);
            return id;
        }

        @Override
        public void update(Employee employee) {
            employees.put(employee.id, employee);
        }

        @Override
        public void delete(Employee employee) {
            employees.remove(employee.id);
        }

        @Override
        public Employee findById(long employeeId) {
            return employees.get(employeeId);
        }

        @Override
        public Employee findByCode(String code) {
            for (Employee e : employees.values()) {
                if (e.code.equals(code)) return e;
            }
            return null;
        }

        @Override
        public Employee findByEmail(String email) {
            for (Employee e : employees.values()) {
                if (e.email.equals(email)) return e;
            }
            return null;
        }

        @Override
        public Employee findByAccountId(long accountId) {
            for (Employee e : employees.values()) {
                if (e.accountId == accountId) return e;
            }
            return null;
        }

        @Override
        public List<Employee> getAll() {
            return new ArrayList<>(employees.values());
        }

        @Override
        public List<Employee> search(String keyword) {
            return new ArrayList<>(employees.values());
        }

        @Override
        public long count() {
            return employees.size();
        }
    }

    private static class FakeSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() {
            return new HashMap<>(values);
        }

        @Override
        public String getString(String key, String defValue) {
            Object v = values.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public Set<String> getStringSet(String key, Set<String> defValues) {
            return defValues;
        }

        @Override
        public int getInt(String key, int defValue) {
            Object v = values.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = values.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = values.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = values.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) {
            return values.containsKey(key);
        }

        @Override
        public Editor edit() {
            return new FakeEditor(values);
        }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private static class FakeEditor implements Editor {
            private final Map<String, Object> target;
            private final Map<String, Object> temp = new HashMap<>();
            private boolean clear = false;

            public FakeEditor(Map<String, Object> target) {
                this.target = target;
            }

            @Override
            public Editor putString(String key, String value) {
                temp.put(key, value);
                return this;
            }

            @Override
            public Editor putStringSet(String key, Set<String> values) {
                temp.put(key, values);
                return this;
            }

            @Override
            public Editor putInt(String key, int value) {
                temp.put(key, value);
                return this;
            }

            @Override
            public Editor putLong(String key, long value) {
                temp.put(key, value);
                return this;
            }

            @Override
            public Editor putFloat(String key, float value) {
                temp.put(key, value);
                return this;
            }

            @Override
            public Editor putBoolean(String key, boolean value) {
                temp.put(key, value);
                return this;
            }

            @Override
            public Editor remove(String key) {
                temp.put(key, this);
                return this;
            }

            @Override
            public Editor clear() {
                clear = true;
                return this;
            }

            @Override
            public boolean commit() {
                apply();
                return true;
            }

            @Override
            public void apply() {
                if (clear) {
                    target.clear();
                }
                for (Map.Entry<String, Object> entry : temp.entrySet()) {
                    if (entry.getValue() == this) {
                        target.remove(entry.getKey());
                    } else {
                        target.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
}
