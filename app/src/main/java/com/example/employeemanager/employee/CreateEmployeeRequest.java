package com.example.employeemanager.employee;

/** Yêu cầu tạo đồng thời hồ sơ và tài khoản; không ghi yêu cầu này vào nhật ký. */
public final class CreateEmployeeRequest extends UpdateEmployeeRequest {
    public final String username, password;

    public CreateEmployeeRequest(String code, String fullname, String email, String phone,
                                 String hourlyRate, String username, String password) {
        super(code, fullname, email, phone, hourlyRate);
        this.username = username;
        this.password = password;
    }
}
