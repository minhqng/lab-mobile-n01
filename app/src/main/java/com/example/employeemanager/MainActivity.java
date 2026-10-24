package com.example.employeemanager;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.auth.AdminPlaceholderActivity;
import com.example.employeemanager.auth.EmployeePlaceholderActivity;
import com.example.employeemanager.auth.LoginActivity;
import com.example.employeemanager.auth.SessionData;
import com.example.employeemanager.auth.SessionManager;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.DataInitializer;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.Constants;

/**
 * Màn hình khởi động và điều phối chính của ứng dụng.
 *
 * <p>Đảm bảo dữ liệu demo ban đầu được khởi tạo an toàn (idempotent), kiểm tra trạng thái
 * phiên đăng nhập từ SharedPreferences có đối chiếu với Room, sau đó điều hướng người dùng
 * tới đúng màn hình tương ứng với vai trò hoặc chuyển về {@link LoginActivity} nếu chưa đăng nhập.</p>
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dispatchSession();
    }

    private void dispatchSession() {
        AppDB appDb = AppDB.getInstance(this);
        SessionManager sessionManager = new SessionManager(this, appDb);

        AppExecutors.databaseExecutor().execute(() -> {
            // 1. Đảm bảo khởi tạo tài khoản quản trị viên và dữ liệu demo nếu cơ sở dữ liệu mới
            DataInitializer.initializeInitialData(appDb);

            // 2. Kiểm tra và xác thực phiên đăng nhập hiện tại
            SessionData session = sessionManager.getValidatedSession();

            // 3. Điều hướng trên luồng giao diện chính
            AppExecutors.runOnMainThread(() -> {
                if (isFinishing() || isDestroyed()) return;

                Intent targetIntent;
                if (session == null) {
                    targetIntent = new Intent(MainActivity.this, LoginActivity.class);
                } else if (Constants.ROLE_ADMIN.equals(session.getRole())) {
                    targetIntent = new Intent(MainActivity.this, AdminPlaceholderActivity.class);
                } else if (Constants.ROLE_EMPLOYEE.equals(session.getRole())) {
                    targetIntent = new Intent(MainActivity.this, EmployeePlaceholderActivity.class);
                } else {
                    // Phiên không hợp lệ, đăng xuất và về màn hình đăng nhập
                    sessionManager.logout();
                    targetIntent = new Intent(MainActivity.this, LoginActivity.class);
                }

                targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(targetIntent);
                finish();
            });
        });
    }
}
