package com.example.employeemanager.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.employeemanager.data.dao.AccountDao;
import com.example.employeemanager.data.dao.EmployeeDao;
import com.example.employeemanager.data.dao.ReportDao;
import com.example.employeemanager.data.dao.SalaryDao;
import com.example.employeemanager.data.dao.WorkLogDao;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.data.entities.WorkLog;

/**
 * Cơ sở dữ liệu Room duy nhất của ứng dụng.
 *
 * <p>Đúng bốn bảng nghiệp vụ: {@code Account}, {@code Employee}, {@code WorkLog},
 * {@code Salary}. Không bật truy vấn trên luồng giao diện và không dùng cơ chế xóa
 * dữ liệu khi thay đổi schema.</p>
 */
@Database(
        entities = {
                Account.class,
                Employee.class,
                WorkLog.class,
                Salary.class
        },
        version = 1,
        exportSchema = true
)
public abstract class AppDB extends RoomDatabase {

    private static final String DATABASE_NAME = "employee_manager.db";

    private static volatile AppDB instance;

    public abstract AccountDao accountDao();

    public abstract EmployeeDao employeeDao();

    public abstract WorkLogDao workLogDao();

    public abstract SalaryDao salaryDao();

    public abstract ReportDao reportDao();

    /**
     * Lấy thể hiện duy nhất của cơ sở dữ liệu, dùng Application Context.
     */
    public static AppDB getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDB.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDB.class,
                                    DATABASE_NAME)
                            .build();
                }
            }
        }
        return instance;
    }
}
