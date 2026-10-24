package com.example.employeemanager.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.DataInitializer;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.util.Constants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Kiểm thử tích hợp Instrumented cho hệ thống Đăng nhập, Quản lý phiên và Phân quyền với Room Database.
 */
@RunWith(AndroidJUnit4.class)
public class AuthInstrumentedTest {

    private AppDB testDb;
    private SharedPreferences testPrefs;
    private SessionManager sessionManager;
    private Context context;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        // Dùng cơ sở dữ liệu in-memory để kiểm thử độc lập, dọn dẹp sạch sau mỗi ca test
        testDb = Room.inMemoryDatabaseBuilder(context, AppDB.class)
                .allowMainThreadQueries()
                .build();

        testPrefs = context.getSharedPreferences("test_auth_prefs", Context.MODE_PRIVATE);
        testPrefs.edit().clear().commit();

        AuthService authService = new AuthService(testDb.accountDao(), testDb.employeeDao());
        sessionManager = new SessionManager(testPrefs, authService, testDb.accountDao(), testDb.employeeDao());
    }

    @After
    public void tearDown() {
        testPrefs.edit().clear().commit();
        if (testDb != null && testDb.isOpen()) {
            testDb.close();
        }
    }

    @Test
    public void testDataInitializerCreatesAdminAndEmployeeInRoom() {
        DataInitializer.initializeInitialData(testDb);

        assertEquals("Phải có đúng 2 tài khoản được khởi tạo", 2, testDb.accountDao().count());
        assertEquals("Phải có đúng 1 nhân viên được khởi tạo", 1, testDb.employeeDao().count());

        Account admin = testDb.accountDao().findByUsername(DataInitializer.DEMO_ADMIN_USERNAME);
        assertNotNull(admin);
        assertEquals(Constants.ROLE_ADMIN, admin.role);
        assertTrue(admin.password.startsWith(PasswordHasher.ALGORITHM_PREFIX));

        // Kiểm tra tính idempotent: chạy lại không tăng số lượng
        DataInitializer.initializeInitialData(testDb);
        assertEquals(2, testDb.accountDao().count());
        assertEquals(1, testDb.employeeDao().count());
    }

    @Test
    public void testAdminLoginAndSessionPersistenceInAndroidContext() {
        DataInitializer.initializeInitialData(testDb);

        AuthResult result = sessionManager.login(DataInitializer.DEMO_ADMIN_USERNAME, DataInitializer.DEMO_ADMIN_RAW_PASSWORD);
        assertTrue("Admin đăng nhập phải thành công", result.isSuccess());

        SessionData session = sessionManager.getValidatedSession();
        assertNotNull("Session phải được khôi phục từ SharedPreferences và đối chiếu Room thành công", session);
        assertEquals(DataInitializer.DEMO_ADMIN_USERNAME, session.getUsername());
        assertEquals(Constants.ROLE_ADMIN, session.getRole());
        assertTrue(session.isAdmin());

        // Kiểm tra phân quyền
        sessionManager.requireRole(Constants.ROLE_ADMIN);
        assertThrows(SecurityException.class, () -> sessionManager.requireRole(Constants.ROLE_EMPLOYEE));
        assertThrows(SecurityException.class, () -> sessionManager.requireCurrentEmployeeId());

        // Đăng xuất
        sessionManager.logout();
        assertNull("Sau khi đăng xuất, session phải bị xóa hoàn toàn", sessionManager.getValidatedSession());
    }

    @Test
    public void testEmployeeLoginAndEmployeeIdLookup() {
        DataInitializer.initializeInitialData(testDb);

        AuthResult result = sessionManager.login(DataInitializer.DEMO_EMPLOYEE_USERNAME, DataInitializer.DEMO_EMPLOYEE_RAW_PASSWORD);
        assertTrue("Employee đăng nhập phải thành công", result.isSuccess());

        SessionData session = sessionManager.getValidatedSession();
        assertNotNull(session);
        assertEquals(DataInitializer.DEMO_EMPLOYEE_USERNAME, session.getUsername());
        assertEquals(Constants.ROLE_EMPLOYEE, session.getRole());
        assertTrue(session.isEmployee());

        long employeeId = sessionManager.requireCurrentEmployeeId();
        assertTrue("Employee ID phải hợp lệ (>0)", employeeId > 0);

        sessionManager.requireRole(Constants.ROLE_EMPLOYEE);
        assertThrows(SecurityException.class, () -> sessionManager.requireRole(Constants.ROLE_ADMIN));
    }

    @Test
    public void testSessionRevocationWhenAccountAlteredInRoom() {
        DataInitializer.initializeInitialData(testDb);
        sessionManager.login(DataInitializer.DEMO_ADMIN_USERNAME, DataInitializer.DEMO_ADMIN_RAW_PASSWORD);
        assertNotNull(sessionManager.getValidatedSession());

        // Thay đổi vai trò của admin trong DB
        Account admin = testDb.accountDao().findByUsername(DataInitializer.DEMO_ADMIN_USERNAME);
        admin.role = Constants.ROLE_EMPLOYEE;
        testDb.accountDao().update(admin);

        // SessionManager phải phát hiện vai trò không khớp và tự động xóa phiên
        assertNull("Phiên phải bị hủy khi vai trò trong DB bị thay đổi", sessionManager.getValidatedSession());
    }
}
