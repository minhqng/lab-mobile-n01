package com.example.employeemanager.attendance;

/**
 * Lỗi nghiệp vụ chấm công, được ném ra thay vì làm ứng dụng bị dừng.
 */
public class AttendanceException extends IllegalArgumentException {

    public AttendanceException(String message) {
        super(message);
    }

    public AttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}