package com.example.employeemanager.employee;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.employeemanager.R;
import java.util.Collections;

public final class EmployeeListActivity extends EmployeeActivity {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable search = this::loadEmployees;
    private int queryVersion;
    private EmployeeAdapter adapter;
    private EditText keyword;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_employee_list);
        prepareScreen();
        keyword = findViewById(R.id.nv_keyword);
        RecyclerView list = findViewById(R.id.nv_list);
        adapter = new EmployeeAdapter(employee -> startActivity(
                new Intent(this, EmployeeDetailActivity.class)
                        .putExtra(EXTRA_EMPLOYEE_ID, employee.id)));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        findViewById(R.id.nv_add).setOnClickListener(view ->
                startActivity(new Intent(this, EmployeeFormActivity.class)));
        findViewById(R.id.nv_retry).setOnClickListener(view -> loadEmployees());
        keyword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryVersion++;
                handler.removeCallbacks(search);
                if (authorized) handler.postDelayed(search, 250);
            }
            @Override public void afterTextChanged(Editable text) { }
        });
    }

    @Override protected void onAuthorized() { loadEmployees(); }

    private void loadEmployees() {
        if (!authorized) return;
        String query = keyword.getText().toString();
        int version = ++queryVersion;
        adapter.submit(Collections.emptyList());
        findViewById(R.id.nv_loading).setVisibility(View.VISIBLE);
        findViewById(R.id.nv_retry).setVisibility(View.GONE);
        ((TextView) findViewById(R.id.nv_status)).setText(R.string.nv_dang_tai);
        runTask(() -> service.searchEmployees(query), employees -> {
            if (version != queryVersion) return;
            findViewById(R.id.nv_loading).setVisibility(View.GONE);
            adapter.submit(employees);
            ((TextView) findViewById(R.id.nv_status)).setText(employees.isEmpty()
                    ? (query.trim().isEmpty() ? R.string.nv_chua_co : R.string.nv_khong_tim_thay)
                    : R.string.nv_danh_sach_da_tai);
        }, error -> {
            if (version != queryVersion) return;
            findViewById(R.id.nv_loading).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.nv_status)).setText(
                    error instanceof EmployeeException ? errorMessage(error) : R.string.nv_loi_tai);
            findViewById(R.id.nv_retry).setVisibility(View.VISIBLE);
        });
    }

    @Override protected void onPause() {
        handler.removeCallbacks(search);
        queryVersion++;
        super.onPause();
    }
}
