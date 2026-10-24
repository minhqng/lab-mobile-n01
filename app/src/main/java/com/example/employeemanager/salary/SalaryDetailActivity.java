package com.example.employeemanager.salary;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.employeemanager.R;
import com.example.employeemanager.data.dto.SalaryDetailDto;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.Constants;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Màn hình chi tiết bảng lương dành cho quản trị viên (ADMIN).
 *
 * <p>Kiến trúc tuân thủ nghiêm ngặt Activity → Service. Mọi thao tác đọc dữ liệu và chốt thanh toán
 * đều thông qua {@link SalaryService} có kiểm tra phân quyền, không gọi trực tiếp DAO.</p>
 */
public class SalaryDetailActivity extends AppCompatActivity {

    public static final String EXTRA_SALARY_ID = "EXTRA_SALARY_ID";

    private ProgressBar pbDetailLoading;
    private View cardSalaryContent;
    private TextView tvDetailEmployeeCode;
    private TextView tvDetailEmployeeName;
    private TextView tvDetailPeriod;
    private TextView tvDetailTotalHours;
    private TextView tvDetailOvertimeHours;
    private TextView tvDetailTotalSalary;
    private TextView tvDetailStatus;
    private Button btnConfirmPaid;

    private SalaryService salaryService;

    private long salaryId = -1L;
    private Salary currentSalary;
    private boolean isOperating = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_detail);

        initViews();
        initData();

        salaryId = getIntent().getLongExtra(EXTRA_SALARY_ID, -1L);
        if (salaryId <= 0) {
            showToast(getString(R.string.luong_error_invalid_id));
            finish();
            return;
        }

        btnConfirmPaid.setOnClickListener(v -> showConfirmPaidDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSalaryDetails();
    }

    private void initViews() {
        pbDetailLoading = findViewById(R.id.pbDetailLoading);
        cardSalaryContent = findViewById(R.id.cardSalaryContent);
        tvDetailEmployeeCode = findViewById(R.id.tvDetailEmployeeCode);
        tvDetailEmployeeName = findViewById(R.id.tvDetailEmployeeName);
        tvDetailPeriod = findViewById(R.id.tvDetailPeriod);
        tvDetailTotalHours = findViewById(R.id.tvDetailTotalHours);
        tvDetailOvertimeHours = findViewById(R.id.tvDetailOvertimeHours);
        tvDetailTotalSalary = findViewById(R.id.tvDetailTotalSalary);
        tvDetailStatus = findViewById(R.id.tvDetailStatus);
        btnConfirmPaid = findViewById(R.id.btnConfirmPaid);
    }

    private void initData() {
        salaryService = SalaryServiceFactory.createService(this);
    }

    private void setOperatingState(boolean operating) {
        this.isOperating = operating;
        btnConfirmPaid.setEnabled(!operating);
        pbDetailLoading.setVisibility(operating ? View.VISIBLE : View.GONE);
    }

    private void loadSalaryDetails() {
        if (isOperating) return;
        setOperatingState(true);

        AppExecutors.databaseExecutor().execute(() -> {
            try {
                // Tải chi tiết bảng lương qua Service có kiểm tra quyền ADMIN
                SalaryDetailDto detail = salaryService.getSalaryDetail(salaryId);
                if (detail == null || detail.getSalary() == null) {
                    AppExecutors.runOnMainThread(() -> {
                        setOperatingState(false);
                        showToast(getString(R.string.luong_error_not_found));
                        finish();
                    });
                    return;
                }

                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    currentSalary = detail.getSalary();
                    displaySalaryInfo(detail.getSalary(), detail.getEmployee());
                });
            } catch (SecurityException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_error_permission_denied));
                    finish();
                });
            } catch (Exception e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_format_error_detail, getString(R.string.luong_error_prefix), e.getMessage()));
                });
            }
        });
    }

    private void displaySalaryInfo(Salary salary, Employee employee) {
        if (employee != null) {
            tvDetailEmployeeCode.setText(employee.code);
            tvDetailEmployeeName.setText(employee.fullname);
        } else {
            tvDetailEmployeeCode.setText("—");
            tvDetailEmployeeName.setText(getString(R.string.luong_fallback_employee, salary.employeeId));
        }

        tvDetailPeriod.setText(getString(R.string.luong_format_period, salary.month, salary.year));

        String totalHoursStr = getString(R.string.luong_format_hours, salary.totalHours, getString(R.string.luong_unit_hours));
        tvDetailTotalHours.setText(totalHoursStr);

        String overtimeHoursStr = getString(R.string.luong_format_hours, salary.overtimeHours, getString(R.string.luong_unit_hours));
        tvDetailOvertimeHours.setText(overtimeHoursStr);

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        String salaryStr = getString(R.string.luong_format_currency, formatter.format(salary.totalSalary), getString(R.string.luong_unit_currency));
        tvDetailTotalSalary.setText(salaryStr);

        if (Constants.STATUS_PAID.equals(salary.status)) {
            tvDetailStatus.setText(R.string.luong_status_paid);
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_paid);
            tvDetailStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            btnConfirmPaid.setEnabled(false);
            btnConfirmPaid.setVisibility(View.GONE);
        } else {
            tvDetailStatus.setText(R.string.luong_status_draft);
            tvDetailStatus.setBackgroundResource(R.drawable.bg_status_draft);
            tvDetailStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark));
            btnConfirmPaid.setEnabled(true);
            btnConfirmPaid.setVisibility(View.VISIBLE);
        }
    }

    private void showConfirmPaidDialog() {
        if (currentSalary == null || isOperating) return;

        new AlertDialog.Builder(this)
                .setTitle(R.string.luong_confirm_paid_title)
                .setMessage(R.string.luong_confirm_paid_message)
                .setPositiveButton(R.string.luong_confirm_yes, (dialog, which) -> performConfirmPaid())
                .setNegativeButton(R.string.luong_confirm_no, null)
                .show();
    }

    private void performConfirmPaid() {
        if (isOperating) return;
        setOperatingState(true);

        AppExecutors.databaseExecutor().execute(() -> {
            try {
                // Chốt thanh toán qua Service có kiểm tra quyền ADMIN
                salaryService.confirmSalaryPaid(salaryId);
                AppExecutors.runOnMainThread(() -> {
                    showToast(getString(R.string.luong_msg_paid_success));
                    loadSalaryDetails();
                });
            } catch (SecurityException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_error_permission_denied));
                });
            } catch (IllegalStateException | IllegalArgumentException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(e.getMessage());
                });
            } catch (Exception e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_format_error_detail, getString(R.string.luong_error_generic), e.getMessage()));
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
