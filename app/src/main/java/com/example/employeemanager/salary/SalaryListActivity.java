package com.example.employeemanager.salary;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;
import com.example.employeemanager.util.AppExecutors;
import com.example.employeemanager.util.Constants;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Màn hình danh sách bảng lương dành cho quản trị viên (ADMIN).
 *
 * <p>Kiến trúc tuân thủ nghiêm ngặt Activity → Service. Mọi thao tác đọc dữ liệu nhân viên
 * và bảng lương đều thông qua {@link SalaryService} có kiểm tra phân quyền, không gọi trực tiếp DAO.</p>
 */
public class SalaryListActivity extends com.example.employeemanager.auth.ProtectedActivity {
    @Override protected String requiredRole() { return com.example.employeemanager.util.Constants.ROLE_ADMIN; }

    private Spinner spMonth;
    private Spinner spYear;
    private Spinner spEmployee;
    private Button btnCalculateSalary;
    private Button btnRefresh;
    private ProgressBar pbLoading;
    private TextView tvEmptyState;
    private RecyclerView rvSalaryList;

    private SalaryAdapter salaryAdapter;
    private SalaryService salaryService;

    private final List<Employee> employeeList = new ArrayList<>();
    private final Map<Long, Employee> employeeMap = new HashMap<>();

    private int selectedMonth;
    private int selectedYear;
    private boolean isOperating = false;
    private int loadGeneration;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_salary_list);

        initViews();
        initData();
        setupSpinners();
        setupRecyclerView();
        setupListeners();
    }

    @Override
    protected void onSessionReady() {
        loadEmployeesAndSalaries();
    }

    private void initViews() {
        spMonth = findViewById(R.id.spMonth);
        spYear = findViewById(R.id.spYear);
        spEmployee = findViewById(R.id.spEmployee);
        btnCalculateSalary = findViewById(R.id.btnCalculateSalary);
        btnRefresh = findViewById(R.id.btnRefresh);
        pbLoading = findViewById(R.id.pbLoading);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        rvSalaryList = findViewById(R.id.rvSalaryList);
    }

    private void initData() {
        salaryService = SalaryServiceFactory.createService(this);

        LocalDate now = LocalDate.now(ZoneId.of(Constants.TIME_ZONE_ID));
        selectedMonth = now.getMonthValue();
        selectedYear = now.getYear();
    }

    private void setupSpinners() {
        // Cấu hình Spinner Tháng (1 - 12) dùng định dạng resource
        List<String> months = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            months.add(getString(R.string.luong_format_month, m));
        }
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMonth.setAdapter(monthAdapter);
        spMonth.setSelection(selectedMonth - 1);

        // Cấu hình Spinner Năm (các năm lân cận)
        List<String> years = new ArrayList<>();
        for (int y = selectedYear - 2; y <= selectedYear + 2; y++) {
            years.add(String.valueOf(y));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spYear.setAdapter(yearAdapter);
        spYear.setSelection(2); // Vị trí năm hiện tại

        AdapterView.OnItemSelectedListener periodChangeListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedMonth = spMonth.getSelectedItemPosition() + 1;
                selectedYear = Integer.parseInt(spYear.getSelectedItem().toString());
                loadSalariesForCurrentPeriod();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        };

        spMonth.setOnItemSelectedListener(periodChangeListener);
        spYear.setOnItemSelectedListener(periodChangeListener);
    }

    private void setupRecyclerView() {
        salaryAdapter = new SalaryAdapter(salary -> {
            Intent intent = new Intent(SalaryListActivity.this, SalaryDetailActivity.class);
            intent.putExtra(SalaryDetailActivity.EXTRA_SALARY_ID, salary.id);
            startActivity(intent);
        });
        rvSalaryList.setLayoutManager(new LinearLayoutManager(this));
        rvSalaryList.setAdapter(salaryAdapter);
    }

    private void setupListeners() {
        btnCalculateSalary.setOnClickListener(v -> performCalculateSalary());
        btnRefresh.setOnClickListener(v -> loadEmployeesAndSalaries());
    }

    private void setOperatingState(boolean operating) {
        this.isOperating = operating;
        btnCalculateSalary.setEnabled(!operating);
        btnRefresh.setEnabled(!operating);
        spMonth.setEnabled(!operating);
        spYear.setEnabled(!operating);
        spEmployee.setEnabled(!operating);
        pbLoading.setVisibility(operating ? View.VISIBLE : View.GONE);
    }

    private void loadEmployeesAndSalaries() {
        if (isOperating) return;
        setOperatingState(true);

        AppExecutors.databaseExecutor().execute(() -> {
            try {
                // Tải danh sách nhân viên qua Service có kiểm tra quyền ADMIN
                List<Employee> employees = salaryService.getEmployeesForSalaryManagement();
                AppExecutors.runOnMainThread(() -> {
                    updateEmployeeSpinner(employees);
                    loadSalariesForCurrentPeriod();
                });
            } catch (SecurityException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_error_permission_denied)); finish();
                });
            } catch (Exception e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.bc_data_error));
                });
            }
        });
    }

    private void updateEmployeeSpinner(List<Employee> employees) {
        employeeList.clear();
        employeeMap.clear();

        List<String> displayList = new ArrayList<>();
        if (employees != null && !employees.isEmpty()) {
            employeeList.addAll(employees);
            for (Employee e : employees) {
                employeeMap.put(e.id, e);
                displayList.add(getString(R.string.luong_format_employee_display, e.code, e.fullname));
            }
        } else {
            displayList.add(getString(R.string.luong_empty_employee));
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, displayList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spEmployee.setAdapter(adapter);
    }

    private void loadSalariesForCurrentPeriod() {
        int expected = ++loadGeneration;
        if (!isOperating) {
            setOperatingState(true);
        }

        final int month = selectedMonth;
        final int year = selectedYear;

        AppExecutors.databaseExecutor().execute(() -> {
            try {
                // Tải danh sách bảng lương qua Service có kiểm tra quyền ADMIN
                List<Salary> salaries = salaryService.getSalariesByMonth(month, year);
                AppExecutors.runOnMainThread(() -> {
                    if (expected != loadGeneration || isFinishing() || isDestroyed()) return;
                    setOperatingState(false);
                    salaryAdapter.setData(salaries, employeeMap);
                    tvEmptyState.setVisibility((salaries == null || salaries.isEmpty())
                            ? View.VISIBLE : View.GONE);
                });
            } catch (SecurityException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_error_permission_denied)); finish();
                });
            } catch (Exception e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.bc_data_error));
                });
            }
        });
    }

    private void performCalculateSalary() {
        if (employeeList.isEmpty()) {
            showToast(getString(R.string.luong_empty_employee));
            return;
        }

        int selectedIndex = spEmployee.getSelectedItemPosition();
        if (selectedIndex < 0 || selectedIndex >= employeeList.size()) {
            showToast(getString(R.string.luong_empty_employee));
            return;
        }

        final Employee selectedEmployee = employeeList.get(selectedIndex);
        final int month = selectedMonth;
        final int year = selectedYear;

        setOperatingState(true);

        AppExecutors.databaseExecutor().execute(() -> {
            try {
                // Tính lương qua Service có kiểm tra quyền ADMIN
                salaryService.generateMonthlySalary(selectedEmployee.id, month, year);
                AppExecutors.runOnMainThread(() -> {
                    showToast(getString(R.string.luong_msg_calculate_success));
                    loadSalariesForCurrentPeriod();
                });
            } catch (SecurityException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.luong_error_permission_denied)); finish();
                });
            } catch (IllegalStateException | IllegalArgumentException e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(e.getMessage());
                });
            } catch (Exception e) {
                AppExecutors.runOnMainThread(() -> {
                    setOperatingState(false);
                    showToast(getString(R.string.bc_data_error));
                });
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
