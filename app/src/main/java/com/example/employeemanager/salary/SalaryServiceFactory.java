package com.example.employeemanager.salary;

import android.content.Context;
import com.example.employeemanager.auth.SessionManager;

import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.util.Constants;

/** Khởi tạo dịch vụ lương bằng phiên Auth đã đối chiếu Room. */
public final class SalaryServiceFactory {

    private static volatile SalaryAuthorization customAuthorization;

    private SalaryServiceFactory() {
    }

    /** Cấu hình điểm tiêm quyền cho kiểm thử. */
    public static void setAuthorization(SalaryAuthorization authorization) {
        customAuthorization = authorization;
    }

    /** Khôi phục bộ kiểm tra phiên production. */
    public static void resetAuthorization() {
        customAuthorization = null;
    }

    /** Khởi tạo dịch vụ lương bằng phiên Auth đã đối chiếu Room. */
    public static SalaryService createService(Context context) {
        AppDB db = AppDB.getInstance(context);
        SalaryAuthorization auth = (customAuthorization != null)
                ? customAuthorization
                : () -> new SessionManager(context, db).requireRole(Constants.ROLE_ADMIN);
        return new SalaryService(db, auth);
    }

}
