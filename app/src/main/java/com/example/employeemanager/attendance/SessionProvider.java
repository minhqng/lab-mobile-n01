package com.example.employeemanager.attendance;

/**
 * Giao diện trừu tượng cho thông tin phiên đăng nhập hiện tại.
 */
public interface SessionProvider {

    long requireCurrentEmployeeId();

    default long getCurrentEmployeeId() {
        return requireCurrentEmployeeId();
    }
}
