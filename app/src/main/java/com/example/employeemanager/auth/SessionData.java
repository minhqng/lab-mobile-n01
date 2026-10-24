package com.example.employeemanager.auth;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.employeemanager.util.Constants;

import java.util.Objects;

/**
 * Dữ liệu phiên đăng nhập đã được xác thực trực tiếp với cơ sở dữ liệu Room.
 *
 * <p>Không tin cậy thông tin chỉ lưu trong SharedPreferences mà luôn được đối chiếu
 * và xác minh lại sự tồn tại cũng như vai trò thực tế của tài khoản trong cơ sở dữ liệu.</p>
 */
public final class SessionData {

    private final long accountId;
    private final String username;
    private final String role;
    private final Long employeeId;

    public SessionData(long accountId, @NonNull String username, @NonNull String role, @Nullable Long employeeId) {
        this.accountId = accountId;
        this.username = Objects.requireNonNull(username, "Tên đăng nhập không được null.");
        this.role = Objects.requireNonNull(role, "Vai trò không được null.");
        this.employeeId = employeeId;
    }

    public long getAccountId() {
        return accountId;
    }

    @NonNull
    public String getUsername() {
        return username;
    }

    @NonNull
    public String getRole() {
        return role;
    }

    @Nullable
    public Long getEmployeeId() {
        return employeeId;
    }

    public boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(role);
    }

    public boolean isEmployee() {
        return Constants.ROLE_EMPLOYEE.equals(role);
    }

    @NonNull
    @Override
    public String toString() {
        return "SessionData{" +
                "accountId=" + accountId +
                ", username='" + username + '\'' +
                ", role='" + role + '\'' +
                ", employeeId=" + employeeId +
                '}';
    }
}
