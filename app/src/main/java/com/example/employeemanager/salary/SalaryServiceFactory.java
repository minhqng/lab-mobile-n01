package com.example.employeemanager.salary;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.util.Constants;

/**
 * Nhà máy khởi tạo và cung cấp {@link SalaryService} cho các màn hình quản lý lương.
 *
 * <p>ĐIỂM TÍCH HỢP BẢO MẬT VỚI THÀNH VIÊN 1:
 * Phân quyền được kiểm tra thông qua {@link SalaryAuthorization}. Mặc định lớp kiểm tra vai trò
 * từ SharedPreferences theo đặc tả (khóa "role"). Khi Thành viên 1 hoàn tất module Authentication/SessionManager,
 * chỉ cần kết nối session thực tế qua phương thức {@link #setAuthorization(SalaryAuthorization)}.</p>
 */
public final class SalaryServiceFactory {

    private static SalaryAuthorization customAuthorization;

    private SalaryServiceFactory() {
    }

    /**
     * Thiết lập cơ chế kiểm tra phân quyền tùy chỉnh (dùng trong kiểm thử hoặc khi tích hợp SessionManager của TV1).
     */
    public static void setAuthorization(SalaryAuthorization authorization) {
        customAuthorization = authorization;
    }

    /**
     * Tạo thể hiện {@link SalaryService} sử dụng cơ sở dữ liệu và bộ phân quyền tương ứng.
     *
     * @param context Context ứng dụng Android.
     * @return đối tượng {@link SalaryService} đã sẵn sàng hoạt động.
     */
    public static SalaryService createService(Context context) {
        AppDB db = AppDB.getInstance(context);
        SalaryAuthorization auth = (customAuthorization != null)
                ? customAuthorization
                : new SharedPreferencesSalaryAuthorization(context.getApplicationContext());
        return new SalaryService(db, auth);
    }

    /**
     * Triển khai phân quyền đọc từ SharedPreferences chuẩn theo Mục 4.1 của đặc tả dự án.
     */
    private static class SharedPreferencesSalaryAuthorization implements SalaryAuthorization {

        private final Context context;

        SharedPreferencesSalaryAuthorization(Context context) {
            this.context = context;
        }

        @Override
        public void requireAdmin() throws SecurityException {
            SharedPreferences prefs = context.getSharedPreferences("app_session", Context.MODE_PRIVATE);
            String role = prefs.getString("role", null);
            if (!Constants.ROLE_ADMIN.equals(role)) {
                // Kiểm tra thêm khóa dự phòng trong trường hợp TV1 dùng tên session khác
                SharedPreferences defaultPrefs = context.getSharedPreferences("session_data", Context.MODE_PRIVATE);
                String fallbackRole = defaultPrefs.getString("role", null);
                if (!Constants.ROLE_ADMIN.equals(fallbackRole)) {
                    throw new SecurityException("Tài khoản hiện tại không có quyền quản trị (ADMIN)");
                }
            }
        }
    }
}
