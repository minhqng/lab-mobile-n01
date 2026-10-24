package com.example.employeemanager.attendance;

/**
 * Hợp đồng SharedPreferences dùng chung giữa module đăng nhập (TV1) và module chấm công (TV3).
 *
 * <p>TV1 phải lưu phiên theo các key này. TV3 chỉ đọc, không nhận employeeId từ Intent/UI.</p>
 */
public final class AttendanceSessionContract {

    /** Tên SharedPreferences lưu phiên đăng nhập. */
    public static final String PREFERENCES_NAME = "employee_manager_session";

    /** Khóa lưu mã tài khoản. */
    public static final String KEY_ACCOUNT_ID = "accountId";

    /** Khóa lưu tên đăng nhập. */
    public static final String KEY_USERNAME = "username";

    /** Khóa lưu vai trò. */
    public static final String KEY_ROLE = "role";

    /** Vai trò nhân viên được phép chấm công. */
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    private AttendanceSessionContract() {
        // Lớp hằng số không được khởi tạo.
    }
}
