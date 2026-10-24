package com.example.employeemanager;

import android.app.Application;
import com.example.employeemanager.auth.EmployeeAuthAdapter;
import com.example.employeemanager.auth.SessionManager;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.employee.EmployeeModule;

/** Kết nối các module với nguồn xác thực duy nhất; callback chỉ truy vấn trên worker. */
public class EmployeeManagerApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        SessionManager sessions = new SessionManager(this, AppDB.getInstance(this));
        EmployeeModule.configure(new EmployeeAuthAdapter(sessions));
    }
}
