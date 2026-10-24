package com.example.employeemanager.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.Constants;
import com.google.android.material.button.MaterialButton;

import java.util.Locale;

/**
 * Màn hình điều phối tạm thời cho vai trò Nhân viên (EMPLOYEE).
 *
 * <p>Bảo vệ phân quyền: kiểm tra vai trò EMPLOYEE và đối chiếu ID nhân viên liên kết
 * thông qua {@link SessionManager#requireCurrentEmployeeId()}. Nếu tài khoản không hợp lệ,
 * tự động đăng xuất và điều hướng về {@link LoginActivity}.</p>
 */
public class EmployeePlaceholderActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvAccountId;
    private TextView tvUsername;
    private TextView tvRole;
    private TextView tvEmployeeId;
    private MaterialButton btnLogout;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_employee_placeholder);

        AppDB appDb = AppDB.getInstance(this);
        sessionManager = new SessionManager(this, appDb);

        tvAccountId = findViewById(R.id.auth_emp_tv_account_id);
        tvUsername = findViewById(R.id.auth_emp_tv_username);
        tvRole = findViewById(R.id.auth_emp_tv_role);
        tvEmployeeId = findViewById(R.id.auth_emp_tv_employee_id);
        btnLogout = findViewById(R.id.auth_emp_btn_logout);

        btnLogout.setOnClickListener(v -> performLogout());

        verifyAndDisplaySession();
    }

    private void verifyAndDisplaySession() {
        AppExecutors.databaseExecutor().execute(() -> {
            try {
                sessionManager.requireRole(Constants.ROLE_EMPLOYEE);
                long employeeId = sessionManager.requireCurrentEmployeeId();
                SessionData session = sessionManager.getValidatedSession();

                if (session == null) {
                    redirectToLogin();
                    return;
                }

                AppExecutors.runOnMainThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvAccountId.setText(String.format(Locale.getDefault(), "Mã tài khoản (Account ID): %d", session.getAccountId()));
                    tvUsername.setText(String.format(Locale.getDefault(), "Tên tài khoản: %s", session.getUsername()));
                    tvRole.setText(String.format(Locale.getDefault(), "Vai trò: %s", session.getRole()));
                    tvEmployeeId.setText(String.format(Locale.getDefault(), "Mã nhân viên (Employee ID): %d", employeeId));
                });
            } catch (Exception e) {
                // Không có quyền hoặc phiên không hợp lệ
                sessionManager.logout();
                redirectToLogin();
            }
        });
    }

    private void performLogout() {
        AppExecutors.databaseExecutor().execute(() -> {
            sessionManager.logout();
            AppExecutors.runOnMainThread(this::redirectToLogin);
        });
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
