package com.example.employeemanager.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Kết quả xác thực đăng nhập trả về từ {@link AuthService}.
 *
 * <p>Đối tượng này tuyệt đối không chứa mật khẩu thô hoặc chuỗi băm mật khẩu để bảo vệ
 * dữ liệu nhạy cảm theo đúng đặc tả bảo mật của dự án.</p>
 */
public final class AuthResult {

    private final boolean success;
    private final String errorMessage;
    private final long accountId;
    private final String username;
    private final String role;

    private AuthResult(boolean success,
                       @Nullable String errorMessage,
                       long accountId,
                       @Nullable String username,
                       @Nullable String role) {
        this.success = success;
        this.errorMessage = errorMessage;
        this.accountId = accountId;
        this.username = username;
        this.role = role;
    }

    /**
     * Tạo kết quả xác thực thành công.
     */
    @NonNull
    public static AuthResult success(long accountId, @NonNull String username, @NonNull String role) {
        return new AuthResult(true, null, accountId, username, role);
    }

    /**
     * Tạo kết quả xác thực thất bại với thông điệp lỗi tiếng Việt.
     */
    @NonNull
    public static AuthResult failure(@NonNull String errorMessage) {
        return new AuthResult(false, errorMessage, -1, null, null);
    }

    public boolean isSuccess() {
        return success;
    }

    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }

    public long getAccountId() {
        return accountId;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getRole() {
        return role;
    }

    @NonNull
    @Override
    public String toString() {
        if (success) {
            return "AuthResult{thanhCong=true, accountId=" + accountId + ", username='" + username + "', role='" + role + "'}";
        } else {
            return "AuthResult{thanhCong=false, loi='" + errorMessage + "'}";
        }
    }
}
