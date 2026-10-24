package com.example.employeemanager.attendance;

import android.content.SharedPreferences;

import com.example.employeemanager.data.dao.AccountDao;
import com.example.employeemanager.data.dao.EmployeeDao;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;

/**
 * Đọc phiên đăng nhập từ SharedPreferences theo {@link AttendanceSessionContract}.
 *
 * <p>Không nhận employeeId từ Intent/UI. Mã tài khoản trong phiên luôn được đối chiếu lại với
 * bảng Account, sau đó tra Employee tương ứng để không tin dữ liệu phiên đã lỗi thời.</p>
 */
public class SharedPreferenceSessionProvider implements SessionProvider {

    private final SharedPreferences preferences;
    private final AccountDao accountDao;
    private final EmployeeDao employeeDao;

    public SharedPreferenceSessionProvider(SharedPreferences preferences,
                                           AccountDao accountDao,
                                           EmployeeDao employeeDao) {
        if (preferences == null || accountDao == null || employeeDao == null) {
            throw new IllegalArgumentException("Thành phần xác thực phiên không được để trống");
        }
        this.preferences = preferences;
        this.accountDao = accountDao;
        this.employeeDao = employeeDao;
    }

    @Override
    public long requireCurrentEmployeeId() {
        if (!preferences.contains(AttendanceSessionContract.KEY_ACCOUNT_ID)) {
            throw new AttendanceException("Chưa có phiên đăng nhập");
        }
        long accountId = preferences.getLong(AttendanceSessionContract.KEY_ACCOUNT_ID, -1L);
        if (accountId <= 0) {
            throw new AttendanceException("Chưa có phiên đăng nhập");
        }

        Account account = accountDao.findById(accountId);
        if (account == null || !Constants.ROLE_EMPLOYEE.equals(account.role)) {
            throw new AttendanceException("Bạn không có quyền chấm công");
        }

        Employee employee = employeeDao.findByAccountId(accountId);
        if (employee == null) {
            throw new AttendanceException("Không tìm thấy nhân viên của phiên đăng nhập");
        }
        return employee.id;
    }
}
