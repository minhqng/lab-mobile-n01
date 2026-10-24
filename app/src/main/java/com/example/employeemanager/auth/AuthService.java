package com.example.employeemanager.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.employeemanager.data.dao.AccountDao;
import com.example.employeemanager.data.dao.EmployeeDao;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;

import java.util.Locale;
import java.util.Objects;

/**
 * Dịch vụ xác thực và quản lý tài khoản người dùng.
 *
 * <p>Mọi thao tác truy vấn cơ sở dữ liệu và băm mật khẩu phải được gọi từ luồng nền
 * (Worker Thread), không thực thi trực tiếp trên luồng giao diện.</p>
 *
 * <h3>Hợp đồng phối hợp dành cho TV2 (Quản lý nhân viên):</h3>
 * <p>Khi TV2 tạo mới nhân viên kèm tài khoản đăng nhập {@code ROLE_EMPLOYEE}, TV2 cần thực hiện
 * trong cùng một Room transaction để đảm bảo tính toàn vẹn (tự động rollback nếu vi phạm
 * trùng lặp thông tin):</p>
 * <pre>{@code
 * appDb.runInTransaction(() -> {
 *     String normalizedUsername = AuthService.normalizeUsername(request.getUsername());
 *     String passwordHash = AuthService.hashPasswordForNewAccount(request.getInitialPassword());
 *     Account newAccount = new Account(normalizedUsername, passwordHash, Constants.ROLE_EMPLOYEE);
 *     long accountId = appDb.accountDao().insert(newAccount);
 *
 *     Employee newEmployee = new Employee(
 *             request.getCode().trim().toUpperCase(Locale.ROOT),
 *             request.getFullname().trim(),
 *             request.getEmail().trim().toLowerCase(Locale.ROOT),
 *             request.getPhone().trim(),
 *             request.getHourlyRate(),
 *             accountId
 *     );
 *     long employeeId = appDb.employeeDao().insert(newEmployee);
 * });
 * }</pre>
 */
public class AuthService {

    private final AccountDao accountDao;
    private final EmployeeDao employeeDao;

    public AuthService(@NonNull AccountDao accountDao, @NonNull EmployeeDao employeeDao) {
        this.accountDao = Objects.requireNonNull(accountDao, "AccountDao không được null.");
        this.employeeDao = Objects.requireNonNull(employeeDao, "EmployeeDao không được null.");
    }

    /**
     * Chuẩn hóa tên đăng nhập: loại bỏ khoảng trắng đầu/cuối và chuyển sang chữ thường
     * sử dụng Locale.ROOT ổn định trên mọi thiết bị.
     */
    @NonNull
    public static String normalizeUsername(@Nullable String username) {
        if (username == null) {
            return "";
        }
        return username.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Tiện ích băm mật khẩu khi tạo mới tài khoản (dành cho TV2 hoặc khởi tạo hệ thống).
     */
    @NonNull
    public static String hashPasswordForNewAccount(@NonNull String plainPassword) {
        return PasswordHasher.hashPassword(plainPassword);
    }

    /**
     * Xác thực thông tin đăng nhập từ tên người dùng và mật khẩu thô.
     *
     * <p>Xác minh mật khẩu thông qua hàm băm có salt {@link PasswordHasher#verifyPassword(String, String)},
     * tuyệt đối không so sánh mật khẩu dạng rõ. Thông báo lỗi không tiết lộ tài khoản có tồn tại hay không.</p>
     *
     * @param username      Tên đăng nhập do người dùng cung cấp.
     * @param plainPassword Mật khẩu thô dạng rõ.
     * @return {@link AuthResult} chứa trạng thái thành công hoặc thông báo lỗi tiếng Việt.
     */
    @WorkerThread
    @NonNull
    public AuthResult login(@Nullable String username, @Nullable String plainPassword) {
        if (username == null || username.trim().isEmpty() || plainPassword == null || plainPassword.isEmpty()) {
            return AuthResult.failure("Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
        }

        String normalizedUsername = normalizeUsername(username);
        Account account = accountDao.findByUsername(normalizedUsername);

        if (account == null) {
            // Không tiết lộ việc tài khoản có tồn tại trong hệ thống hay không để chống tấn công dò tìm tài khoản.
            return AuthResult.failure("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        boolean passwordMatches = PasswordHasher.verifyPassword(plainPassword, account.password);
        if (!passwordMatches) {
            return AuthResult.failure("Tên đăng nhập hoặc mật khẩu không đúng.");
        }

        // Kiểm tra tính hợp lệ của vai trò
        if (!Constants.ROLE_ADMIN.equals(account.role) && !Constants.ROLE_EMPLOYEE.equals(account.role)) {
            return AuthResult.failure("Tài khoản không có quyền truy cập hợp lệ.");
        }

        return AuthResult.success(account.id, account.username, account.role);
    }

    /**
     * Tìm ID nhân viên {@code employeeId} tương ứng với mã tài khoản {@code accountId}.
     *
     * @param accountId ID tài khoản đăng nhập.
     * @return {@code employeeId} nếu tìm thấy nhân viên liên kết, ngược lại trả về -1L.
     */
    @WorkerThread
    public long findEmployeeIdByAccountId(long accountId) {
        if (accountId <= 0) {
            return -1L;
        }
        Employee employee = employeeDao.findByAccountId(accountId);
        return employee != null ? employee.id : -1L;
    }
}
