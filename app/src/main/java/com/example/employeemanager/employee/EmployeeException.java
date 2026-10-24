package com.example.employeemanager.employee;

import androidx.annotation.StringRes;

/** Lỗi nghiệp vụ ánh xạ sang tài nguyên tiếng Việt, không chứa dữ liệu nhạy cảm. */
public final class EmployeeException extends RuntimeException {
    public final String field;
    @StringRes public final int messageRes;

    public EmployeeException(String field, @StringRes int messageRes) {
        this.field = field;
        this.messageRes = messageRes;
    }
}
