package com.example.employeemanager.util;

/**
 * Hằng số dùng chung cho toàn bộ ứng dụng.
 *
 * <p>Không tạo bản sao của các giá trị này trong module khác. Vai trò và trạng thái
 * được lưu đúng bằng chữ hoa như khai báo trong đặc tả.</p>
 */
public final class Constants {

    /** Vai trò quản trị viên. */
    public static final String ROLE_ADMIN = "ADMIN";

    /** Vai trò nhân viên. */
    public static final String ROLE_EMPLOYEE = "EMPLOYEE";

    /** Phiên chấm công đang diễn ra. */
    public static final String STATUS_OPEN = "OPEN";

    /** Phiên chấm công đã hoàn tất. */
    public static final String STATUS_COMPLETED = "COMPLETED";

    /** Bảng lương đang ở trạng thái nháp. */
    public static final String STATUS_DRAFT = "DRAFT";

    /** Bảng lương đã được đánh dấu thanh toán. */
    public static final String STATUS_PAID = "PAID";

    /** Múi giờ nghiệp vụ cố định của ứng dụng. */
    public static final String TIME_ZONE_ID = "Asia/Ho_Chi_Minh";

    /** Số phút làm việc chuẩn của một ngày (8 giờ). */
    public static final int STANDARD_MINUTES_PER_DAY = 480;

    /** Hệ số nhân cho giờ tăng ca. */
    public static final double OVERTIME_MULTIPLIER = 1.5;

    /** Đơn vị tiền tệ sử dụng trong ứng dụng. */
    public static final String CURRENCY_UNIT = "VND";

    /** Định dạng lưu ngày trong cơ sở dữ liệu. */
    public static final String DATE_STORAGE_PATTERN = "yyyy-MM-dd";

    /** Định dạng lưu giờ trong cơ sở dữ liệu. */
    public static final String TIME_STORAGE_PATTERN = "HH:mm:ss";

    /** Định dạng hiển thị ngày cho người dùng. */
    public static final String DATE_DISPLAY_PATTERN = "dd/MM/yyyy";

    /** Định dạng hiển thị giờ cho người dùng. */
    public static final String TIME_DISPLAY_PATTERN = "HH:mm";

    private Constants() {
        // Lớp hằng số không được khởi tạo.
    }
}
