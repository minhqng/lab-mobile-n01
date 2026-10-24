package com.example.employeemanager.auth;

import android.content.Intent;
import com.example.employeemanager.reports.AdminDashboardActivity;
import com.example.employeemanager.reports.EmployeeDashboardActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.Constants;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Màn hình đăng nhập hệ thống dành cho cả hai vai trò ADMIN và EMPLOYEE.
 *
 * <p>Kiểm tra dữ liệu đầu vào, hiển thị trạng thái tải, ngăn chặn gửi nhiều yêu cầu đồng thời,
 * và điều hướng người dùng tới màn hình tương ứng với vai trò sau khi xác thực thành công.</p>
 */
public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilUsername;
    private TextInputLayout tilPassword;
    private TextInputEditText etUsername;
    private TextInputEditText etPassword;
    private TextView tvError;
    private ProgressBar progressBar;
    private MaterialButton btnLogin;

    private SessionManager sessionManager;
    private LoginState loginState;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.auth_activity_login);

        AppDB appDb = AppDB.getInstance(this);
        sessionManager = new SessionManager(this, appDb);

        initViews();
        setupListeners();
        loginState = new androidx.lifecycle.ViewModelProvider(this).get(LoginState.class);
        loginState.loading.observe(this, this::setLoadingState);
        loginState.result.observe(this, result -> {
            if (result == null) return;
            loginState.result.setValue(null);
            if (result.isSuccess()) navigateToRoleDashboard(result.getRole());
            else {
                etPassword.setText("");
                tvError.setText(result.getErrorMessage());
                tvError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void initViews() {
        tilUsername = findViewById(R.id.auth_til_username);
        tilPassword = findViewById(R.id.auth_til_password);
        etUsername = findViewById(R.id.auth_et_username);
        etPassword = findViewById(R.id.auth_et_password);
        tvError = findViewById(R.id.auth_tv_error);
        progressBar = findViewById(R.id.auth_progress_bar);
        btnLogin = findViewById(R.id.auth_btn_login);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> performLogin());
    }

    private void performLogin() {
        // Xóa thông báo lỗi trước đó
        tilUsername.setError(null);
        tilPassword.setError(null);
        tvError.setVisibility(View.GONE);

        String username = etUsername.getText() != null ? etUsername.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        boolean hasError = false;

        if (username.isEmpty()) {
            tilUsername.setError(getString(R.string.auth_error_empty_username));
            hasError = true;
        }

        if (password.isEmpty()) {
            tilPassword.setError(getString(R.string.auth_error_empty_password));
            hasError = true;
        }

        if (hasError) {
            return;
        }

        loginState.login(sessionManager, username, password, getString(R.string.bc_data_error));
    }

    private void setLoadingState(boolean isLoading) {
        btnLogin.setEnabled(!isLoading);
        etUsername.setEnabled(!isLoading);
        etPassword.setEnabled(!isLoading);
        progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void navigateToRoleDashboard(String role) {
        Intent intent;
        if (Constants.ROLE_ADMIN.equals(role)) {
            intent = new Intent(this, AdminDashboardActivity.class);
        } else if (Constants.ROLE_EMPLOYEE.equals(role)) {
            intent = new Intent(this, EmployeeDashboardActivity.class);
        } else {
            // Vai trò không hợp lệ, đăng xuất ngay
            sessionManager.logout();
            tvError.setText(R.string.auth_error_empty_fields);
            tvError.setVisibility(View.VISIBLE);
            return;
        }

        // Xóa toàn bộ stack Activity để nút Back không quay lại màn hình đăng nhập
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
