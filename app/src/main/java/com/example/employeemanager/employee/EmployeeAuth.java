package com.example.employeemanager.employee;

/** Ranh giới TV1: lấy ID từ phiên đã xác thực, không lấy từ Intent. */
public interface EmployeeAuth {
    long currentAccountId();

    /** Băm chậm có salt bằng PasswordHasher của TV1; không trả mật khẩu rõ. */
    String hashPassword(String plainPassword);
}
