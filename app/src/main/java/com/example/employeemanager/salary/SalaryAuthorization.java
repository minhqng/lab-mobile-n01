package com.example.employeemanager.salary;

/**
 * Hợp đồng xác thực phân quyền tối thiểu dành cho module tính toán và quản lý lương.
 *
 * <p>Các phương thức quản trị bảng lương (tính lương, truy vấn danh sách, chốt thanh toán)
 * bắt buộc phải kiểm tra thông qua hợp đồng này trước khi xử lý. Khi Thành viên 1 hoàn tất
 * module Authentication/SessionManager, hợp đồng này sẽ được triển khai bằng session thực.</p>
 */
public interface SalaryAuthorization {

    /**
     * Xác minh rằng người dùng hiện tại có vai trò quản trị viên (ADMIN).
     *
     * @throws SecurityException nếu người dùng không có quyền quản trị hoặc chưa đăng nhập.
     */
    void requireAdmin() throws SecurityException;
}
