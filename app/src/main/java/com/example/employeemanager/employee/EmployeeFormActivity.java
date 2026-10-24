package com.example.employeemanager.employee;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import com.example.employeemanager.R;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Locale;

public final class EmployeeFormActivity extends EmployeeActivity {
    private final Map<String, EditText> fields = new LinkedHashMap<>();
    private long employeeId;
    private boolean editing, loaded;
    private EmployeeMutation mutation;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_employee_form);
        prepareScreen();
        mutation = new ViewModelProvider(this).get(EmployeeMutation.class);
        editing = getIntent().hasExtra(EXTRA_EMPLOYEE_ID);
        employeeId = getIntent().getLongExtra(EXTRA_EMPLOYEE_ID, 0);
        loaded = state != null && state.getBoolean("loaded");
        fields.put("code", findViewById(R.id.nv_code));
        fields.put("fullname", findViewById(R.id.nv_fullname));
        fields.put("email", findViewById(R.id.nv_email));
        fields.put("phone", findViewById(R.id.nv_phone));
        fields.put("hourlyRate", findViewById(R.id.nv_hourly_rate));
        fields.put("username", findViewById(R.id.nv_username));
        fields.put("password", findViewById(R.id.nv_password));
        fields.put("confirmation", findViewById(R.id.nv_confirmation));
        ((TextView) findViewById(R.id.nv_title)).setText(
                editing ? R.string.nv_chinh_sua : R.string.nv_them_moi);
        findViewById(R.id.nv_account_fields).setVisibility(editing ? View.GONE : View.VISIBLE);
        findViewById(R.id.nv_save).setOnClickListener(view -> save());
        findViewById(R.id.nv_retry).setOnClickListener(view -> onAuthorized());
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("loaded", loaded);
        super.onSaveInstanceState(state);
    }

    @Override protected void onAuthorized() {
        setBusy(mutation.isRunning());
        boolean delivered = mutation.attach(error -> {
            if (error == null) {
                fields.get("password").setText("");
                fields.get("confirmation").setText("");
                toast(R.string.nv_luu_thanh_cong);
                setResult(RESULT_OK);
                finish();
            } else {
                setBusy(false);
                formError(error);
            }
        });
        if (delivered || isFinishing() || mutation.isRunning()) return;
        if (!editing || loaded) { setBusy(false); return; }
        setBusy(true);
        runTask(() -> service.getEmployeeById(employeeId), employee -> {
            fields.get("code").setText(employee.code);
            fields.get("fullname").setText(employee.fullname);
            fields.get("email").setText(employee.email);
            fields.get("phone").setText(employee.phone);
            fields.get("hourlyRate").setText(String.format(Locale.ROOT, "%d", employee.hourlyRate));
            loaded = true;
            setBusy(false);
        }, error -> {
            setBusy(false);
            findViewById(R.id.nv_save).setEnabled(false);
            findViewById(R.id.nv_retry).setVisibility(View.VISIBLE);
            showError(error);
        });
    }

    private String value(String field) { return fields.get(field).getText().toString(); }

    private void save() {
        if (mutation.isRunning() || !authorized || (editing && !loaded)) return;
        for (EditText field : fields.values()) field.setError(null);
        UpdateEmployeeRequest request = editing
                ? new UpdateEmployeeRequest(value("code"), value("fullname"), value("email"),
                        value("phone"), value("hourlyRate"))
                : new CreateEmployeeRequest(value("code"), value("fullname"), value("email"),
                        value("phone"), value("hourlyRate"), value("username"), value("password"));
        try {
            EmployeeValidator.validate(request);
            if (!editing) {
                EmployeeValidator.validateAccount((CreateEmployeeRequest) request);
                if (!value("password").equals(value("confirmation")))
                    throw new EmployeeException("confirmation", R.string.nv_mat_khau_khong_khop);
            }
        } catch (EmployeeException error) { formError(error); return; }
        setBusy(true);
        EmployeeService workerService = service;
        boolean isEditing = editing;
        long id = employeeId;
        mutation.start(() -> isEditing ? workerService.updateEmployee(id, request)
                : workerService.createEmployee((CreateEmployeeRequest) request));
    }

    @Override protected void onPause() {
        mutation.detach();
        super.onPause();
    }

    private void formError(Exception error) {
        showError(error);
        if (error instanceof EmployeeException) {
            EditText field = fields.get(((EmployeeException) error).field);
            if (field != null) {
                field.setError(getString(errorMessage(error)));
                field.requestFocus();
            }
        }
    }

    private void setBusy(boolean busy) {
        findViewById(R.id.nv_loading).setVisibility(busy ? View.VISIBLE : View.GONE);
        findViewById(R.id.nv_save).setEnabled(!busy);
        findViewById(R.id.nv_retry).setVisibility(View.GONE);
        for (EditText field : fields.values()) field.setEnabled(!busy);
        ((TextView) findViewById(R.id.nv_status)).setText(busy ? R.string.nv_dang_xu_ly : R.string.nv_huong_dan_form);
    }
}
