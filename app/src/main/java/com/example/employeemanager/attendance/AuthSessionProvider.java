package com.example.employeemanager.attendance;

import com.example.employeemanager.auth.SessionManager;

/** Adapter không lưu thêm phiên và luôn đối chiếu tài khoản với Room. */
public final class AuthSessionProvider implements SessionProvider {
    private final SessionManager sessions;
    public AuthSessionProvider(SessionManager sessions) { this.sessions = sessions; }
    @Override public long requireCurrentEmployeeId() {
        return sessions.requireCurrentEmployeeId();
    }
}
