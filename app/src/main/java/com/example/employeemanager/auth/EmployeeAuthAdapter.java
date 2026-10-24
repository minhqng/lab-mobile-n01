package com.example.employeemanager.auth;

import com.example.employeemanager.employee.EmployeeAuth;

/** Adapter dùng chung giữa khởi tạo ứng dụng và kiểm thử tích hợp. */
public final class EmployeeAuthAdapter implements EmployeeAuth {
    private final SessionManager sessions;
    public EmployeeAuthAdapter(SessionManager sessions) { this.sessions = sessions; }
    @Override public long currentAccountId() {
        SessionData session = sessions.getValidatedSession();
        return session == null ? 0 : session.getAccountId();
    }
    @Override public String hashPassword(String password) {
        return AuthService.hashPasswordForNewAccount(password);
    }
}
