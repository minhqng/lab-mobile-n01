package com.example.employeemanager.employee;

/** Dữ liệu nhập; đơn giá giữ dạng chuỗi để kiểm tra cả lỗi số và tràn miền. */
public class UpdateEmployeeRequest {
    public final String code, fullname, email, phone, hourlyRate;

    public UpdateEmployeeRequest(String code, String fullname, String email,
                                 String phone, String hourlyRate) {
        this.code = code;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.hourlyRate = hourlyRate;
    }
}
