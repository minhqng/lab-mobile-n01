package com.example.employeemanager.employee;

import android.content.Context;
import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import java.util.Objects;

/** TV1 cấu hình một lần từ Application, bằng adapter Auth thật của ứng dụng. */
public final class EmployeeModule {
    private static volatile EmployeeAuth auth = new EmployeeAuth() {
        @Override public long currentAccountId() { return 0; }
        @Override public String hashPassword(String plainPassword) {
            throw new EmployeeException(null, R.string.nv_auth_chua_san_sang);
        }
    };

    private EmployeeModule() { }

    public static void configure(EmployeeAuth authentication) {
        auth = Objects.requireNonNull(authentication);
    }

    public static EmployeeService createService(Context context) {
        return new EmployeeService(AppDB.getInstance(context), auth);
    }
}
