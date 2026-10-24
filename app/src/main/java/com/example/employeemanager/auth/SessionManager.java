package com.example.employeemanager.auth;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.dao.AccountDao;
import com.example.employeemanager.data.dao.EmployeeDao;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;

import java.util.Objects;

/**
 * Quản lý phiên đăng nhập của người dùng qua SharedPreferences và xác thực với Room.
 *
 * <p>Theo Mục 10.1 và Mục IV của đặc tả:
 * <ul>
 *     <li>Sử dụng SharedPreferences ở chế độ {@code MODE_PRIVATE}.</li>
 *     <li>Tuyệt đối không lưu mật khẩu rõ, muối ngẫu nhiên (salt) hoặc chuỗi băm mật khẩu.</li>
 *     <li>Không tin cậy vai trò (role) lưu trong SharedPreferences làm nguồn phân quyền duy nhất.
 *         Mọi thao tác yêu cầu phiên phải được đối chiếu trực tiếp với Room.</li>
 *     <li>Nếu tài khoản bị xóa hoặc thay đổi vai trò trong cơ sở dữ liệu, phiên lập tức bị vô hiệu hóa.</li>
 *     <li>Mọi thao tác truy vấn cơ sở dữ liệu phải được thực thi trên luồng nền (Worker Thread).</li>
 * </ul>
 * </p>
 */
public class SessionManager {

    private static final String PREF_NAME = "auth_session_preferences";

    private static final String KEY_ACCOUNT_ID = "account_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_ROLE = "role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private final SharedPreferences sharedPreferences;
    private final AuthService authService;
    private final AccountDao accountDao;
    private final EmployeeDao employeeDao;

    public SessionManager(@NonNull Context context, @NonNull AppDB appDb) {
        Objects.requireNonNull(context, "Context không được null.");
        Objects.requireNonNull(appDb, "AppDB không được null.");
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.accountDao = appDb.accountDao();
        this.employeeDao = appDb.employeeDao();
        this.authService = new AuthService(accountDao, employeeDao);
    }

    public SessionManager(@NonNull SharedPreferences sharedPreferences,
                          @NonNull AuthService authService,
                          @NonNull AccountDao accountDao,
                          @NonNull EmployeeDao employeeDao) {
        this.sharedPreferences = Objects.requireNonNull(sharedPreferences, "SharedPreferences không được null.");
        this.authService = Objects.requireNonNull(authService, "AuthService không được null.");
        this.accountDao = Objects.requireNonNull(accountDao, "AccountDao không được null.");
        this.employeeDao = Objects.requireNonNull(employeeDao, "EmployeeDao không được null.");
    }

    /**
     * Thực hiện đăng nhập và lưu phiên nếu thành công.
     *
     * <p>Phải được gọi trên luồng nền.</p>
     *
     * @param username      Tên đăng nhập.
     * @param plainPassword Mật khẩu thô dạng rõ.
     * @return Kết quả xác thực {@link AuthResult}.
     */
    @WorkerThread
    @NonNull
    public AuthResult login(@Nullable String username, @Nullable String plainPassword) {
        AuthResult result = authService.login(username, plainPassword);
        if (result.isSuccess()) {
            sharedPreferences.edit()
                    .putLong(KEY_ACCOUNT_ID, result.getAccountId())
                    .putString(KEY_USERNAME, result.getUsername())
                    .putString(KEY_ROLE, result.getRole())
                    .putBoolean(KEY_IS_LOGGED_IN, true)
                    .commit();
        }
        return result;
    }

    /**
     * Đăng xuất và xóa toàn bộ dữ liệu phiên lưu trữ trong SharedPreferences.
     */
    public void logout() {
        sharedPreferences.edit().clear().commit();
    }

    /**
     * Đọc và xác thực phiên đăng nhập hiện tại trực tiếp từ cơ sở dữ liệu Room.
     *
     * <p>Nếu tài khoản không còn tồn tại hoặc vai trò trong DB đã bị thay đổi, phiên
     * sẽ bị xóa và phương thức trả về {@code null}. Phải được gọi trên luồng nền.</p>
     *
     * @return {@link SessionData} hợp lệ, hoặc {@code null} nếu chưa đăng nhập hoặc phiên bị vô hiệu.
     */
    @WorkerThread
    @Nullable
    public SessionData getValidatedSession() {
        boolean isLoggedIn = sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
        long savedAccountId = sharedPreferences.getLong(KEY_ACCOUNT_ID, -1L);
        String savedRole = sharedPreferences.getString(KEY_ROLE, null);

        if (!isLoggedIn || savedAccountId <= 0 || savedRole == null) {
            return null;
        }

        // Đối chiếu trực tiếp với Room để bảo vệ an toàn phân quyền
        Account account = accountDao.findById(savedAccountId);
        if (account == null || !savedRole.equals(account.role)
                || (!Constants.ROLE_ADMIN.equals(account.role)
                && !Constants.ROLE_EMPLOYEE.equals(account.role))) {
            // Dữ liệu trong DB không khớp với phiên SharedPreferences (đã bị xóa hoặc đổi role)
            logout();
            return null;
        }

        Long employeeId = null;
        if (Constants.ROLE_EMPLOYEE.equals(account.role)) {
            Employee employee = employeeDao.findByAccountId(account.id);
            if (employee != null) {
                employeeId = employee.id;
            }
        }

        return new SessionData(account.id, account.username, account.role, employeeId);
    }

    /**
     * Lấy ID nhân viên của tài khoản đang đăng nhập.
     *
     * <p>Phải được gọi trên luồng nền.</p>
     *
     * @return ID nhân viên {@code employeeId}.
     * @throws SecurityException     nếu chưa đăng nhập hoặc tài khoản không có vai trò EMPLOYEE.
     * @throws IllegalStateException nếu tài khoản EMPLOYEE chưa liên kết với bản ghi nhân viên nào.
     */
    @WorkerThread
    public long requireCurrentEmployeeId() {
        SessionData session = getValidatedSession();
        if (session == null) {
            throw new SecurityException("Phiên làm việc không hợp lệ hoặc người dùng chưa đăng nhập.");
        }
        if (!session.isEmployee()) {
            throw new SecurityException("Chức năng này chỉ dành riêng cho nhân viên.");
        }

        Long employeeId = session.getEmployeeId();
        if (employeeId == null || employeeId <= 0) {
            throw new IllegalStateException("Không tìm thấy thông tin nhân viên liên kết với tài khoản này.");
        }

        return employeeId;
    }

    /**
     * Bắt buộc người dùng hiện tại phải có vai trò chỉ định.
     *
     * <p>Phải được gọi trên luồng nền.</p>
     *
     * @param requiredRole Vai trò bắt buộc (ví dụ {@link Constants#ROLE_ADMIN}).
     * @throws SecurityException nếu chưa đăng nhập hoặc vai trò không khớp.
     */
    @WorkerThread
    public void requireRole(@NonNull String requiredRole) {
        Objects.requireNonNull(requiredRole, "Vai trò yêu cầu không được null.");
        SessionData session = getValidatedSession();
        if (session == null) {
            throw new SecurityException("Phiên làm việc không hợp lệ hoặc người dùng chưa đăng nhập.");
        }
        if (!requiredRole.equals(session.getRole())) {
            throw new SecurityException("Tài khoản không có quyền thực hiện chức năng này (yêu cầu vai trò: " + requiredRole + ").");
        }
    }
}
