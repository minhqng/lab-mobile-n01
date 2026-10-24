package com.example.employeemanager.data;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.example.employeemanager.auth.PasswordHasher;
import com.example.employeemanager.data.dao.AccountDao;
import com.example.employeemanager.data.dao.EmployeeDao;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;

import java.util.Locale;

/**
 * Tiện ích khởi tạo dữ liệu mẫu ngoại tuyến ban đầu cho ứng dụng.
 *
 * <p>Theo Mục 14 của tài liệu đặc tả, khi hệ thống khởi động lần đầu và chưa có tài khoản,
 * một tài khoản Quản trị viên (ADMIN) và một tài khoản Nhân viên mẫu (EMPLOYEE) sẽ được
 * tạo trong một Room transaction. Mọi mật khẩu đều được băm bằng {@link PasswordHasher}
 * trước khi lưu, tuyệt đối không lưu mật khẩu rõ trong cơ sở dữ liệu.</p>
 *
 * <p>Cơ chế khởi tạo hoàn toàn idempotent: kiểm tra sự tồn tại trước khi chèn, việc mở lại
 * ứng dụng nhiều lần sẽ không tạo trùng dữ liệu.</p>
 */
public final class DataInitializer {

    /** Tên đăng nhập mặc định của tài khoản Quản trị viên demo. */
    public static final String DEMO_ADMIN_USERNAME = "admin";

    /** Mật khẩu mặc định của tài khoản Quản trị viên demo (chỉ dùng thử nghiệm). */
    public static final String DEMO_ADMIN_RAW_PASSWORD = "QuanLy@2026!";

    /** Tên đăng nhập mặc định của tài khoản Nhân viên demo. */
    public static final String DEMO_EMPLOYEE_USERNAME = "nv001";

    /** Mật khẩu mặc định của tài khoản Nhân viên demo (chỉ dùng thử nghiệm). */
    public static final String DEMO_EMPLOYEE_RAW_PASSWORD = "NhanVien@2026!";

    private DataInitializer() {
        // Lớp tiện ích tĩnh, không được khởi tạo.
    }

    /**
     * Đảm bảo tài khoản Quản trị viên và dữ liệu demo đã tồn tại trong cơ sở dữ liệu.
     *
     * <p>Phải được gọi trên luồng nền (Worker Thread).</p>
     *
     * @param appDb Đối tượng cơ sở dữ liệu Room.
     */
    @WorkerThread
    public static void initializeInitialData(@NonNull AppDB appDb) {
        appDb.runInTransaction(() -> {
            AccountDao accountDao = appDb.accountDao();
            EmployeeDao employeeDao = appDb.employeeDao();

            // Chỉ seed cơ sở dữ liệu mới; không tái tạo nhân viên đã được xóa hợp lệ.
            if (accountDao.count() != 0) return;

            // 1. Khởi tạo tài khoản Quản trị viên nếu chưa có
            Account existingAdmin = accountDao.findByUsername(DEMO_ADMIN_USERNAME);
            if (existingAdmin == null) {
                String adminHash = PasswordHasher.hashPassword(DEMO_ADMIN_RAW_PASSWORD);
                Account adminAccount = new Account(
                        DEMO_ADMIN_USERNAME,
                        adminHash,
                        Constants.ROLE_ADMIN
                );
                accountDao.insert(adminAccount);
            }

            // 2. Khởi tạo tài khoản Nhân viên mẫu theo Mục 14.1 nếu chưa có
            Account existingEmployeeAccount = accountDao.findByUsername(DEMO_EMPLOYEE_USERNAME);
            if (existingEmployeeAccount == null) {
                String employeeHash = PasswordHasher.hashPassword(DEMO_EMPLOYEE_RAW_PASSWORD);
                Account employeeAccount = new Account(
                        DEMO_EMPLOYEE_USERNAME,
                        employeeHash,
                        Constants.ROLE_EMPLOYEE
                );
                long accountId = accountDao.insert(employeeAccount);

                // Kiểm tra và tạo bản ghi Employee liên kết với tài khoản trên
                Employee existingEmployee = employeeDao.findByCode("NV001");
                if (existingEmployee == null) {
                    Employee sampleEmployee = new Employee(
                            "NV001",
                            "Nguyễn Văn An",
                            "an.nv001@example.com",
                            "0912345678",
                            50_000L,
                            accountId
                    );
                    employeeDao.insert(sampleEmployee);
                }
            }
        });
    }
}
