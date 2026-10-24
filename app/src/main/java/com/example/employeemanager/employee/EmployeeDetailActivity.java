package com.example.employeemanager.employee;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;
import com.example.employeemanager.R;

public final class EmployeeDetailActivity extends EmployeeActivity {
    private long employeeId;
    private EmployeeMutation mutation;
    private AlertDialog deleteDialog;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_employee_detail);
        prepareScreen();
        mutation = new ViewModelProvider(this).get(EmployeeMutation.class);
        employeeId = getIntent().getLongExtra(EXTRA_EMPLOYEE_ID, 0);
        findViewById(R.id.nv_edit).setOnClickListener(view -> startActivity(
                new Intent(this, EmployeeFormActivity.class).putExtra(EXTRA_EMPLOYEE_ID, employeeId)));
        findViewById(R.id.nv_delete).setOnClickListener(view -> {
            if (!authorized || mutation.isRunning()) return;
            deleteDialog = new AlertDialog.Builder(this).setTitle(R.string.nv_xoa)
                    .setMessage(R.string.nv_xac_nhan_xoa)
                    .setNegativeButton(R.string.nv_huy, null)
                    .setPositiveButton(R.string.nv_xoa, (dialog, which) -> delete()).show();
        });
        findViewById(R.id.nv_retry).setOnClickListener(view -> onAuthorized());
    }

    @Override protected void onAuthorized() {
        setBusy(true);
        boolean delivered = mutation.attach(error -> {
            if (error == null) {
                toast(R.string.nv_xoa_thanh_cong);
                finish();
            } else {
                setBusy(false);
                showError(error);
            }
        });
        if (delivered || isFinishing() || mutation.isRunning()) return;
        runTask(() -> service.getEmployeeById(employeeId), employee -> {
            ((TextView) findViewById(R.id.nv_code)).setText(employee.code);
            ((TextView) findViewById(R.id.nv_fullname)).setText(employee.fullname);
            ((TextView) findViewById(R.id.nv_email)).setText(employee.email);
            ((TextView) findViewById(R.id.nv_phone)).setText(employee.phone);
            ((TextView) findViewById(R.id.nv_hourly_rate)).setText(
                    EmployeeAdapter.formatRate(findViewById(R.id.nv_root), employee.hourlyRate));
            setBusy(false);
            ((TextView) findViewById(R.id.nv_status)).setText(R.string.nv_chi_tiet_da_tai);
        }, error -> {
            setBusy(false);
            findViewById(R.id.nv_edit).setEnabled(false);
            findViewById(R.id.nv_delete).setEnabled(false);
            findViewById(R.id.nv_retry).setVisibility(View.VISIBLE);
            showError(error);
        });
    }

    private void delete() {
        if (!authorized || mutation.isRunning()) return;
        setBusy(true);
        EmployeeService workerService = service;
        long id = employeeId;
        mutation.start(() -> { workerService.deleteEmployee(id); return true; });
    }

    @Override protected void onPause() {
        mutation.detach();
        if (deleteDialog != null) deleteDialog.dismiss();
        super.onPause();
    }

    private void setBusy(boolean busy) {
        findViewById(R.id.nv_loading).setVisibility(busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.nv_edit).setEnabled(!busy);
        findViewById(R.id.nv_delete).setEnabled(!busy);
        findViewById(R.id.nv_retry).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.nv_status)).setText(busy ? R.string.nv_dang_xu_ly : R.string.nv_chi_tiet_da_tai);
    }
}
