package com.example.employeemanager.employee;

import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.employeemanager.R;
import com.example.employeemanager.util.AppExecutors;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Chạy tác vụ nền và loại bỏ kết quả khi màn hình đã rời vòng đời hiện tại. */
public abstract class EmployeeActivity extends AppCompatActivity {
    public static final String EXTRA_EMPLOYEE_ID = "employeeId";
    protected EmployeeService service;
    private int generation;
    private boolean active;
    protected boolean authorized;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        service = EmployeeModule.createService(this);
    }

    protected void prepareScreen() {
        View root = findViewById(R.id.nv_root);
        int left = root.getPaddingLeft(), top = root.getPaddingTop();
        int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
        findViewById(R.id.nv_back).setOnClickListener(view -> finish());
        root.setVisibility(View.INVISIBLE);
    }

    @Override protected void onResume() {
        super.onResume();
        active = true;
        authorized = false;
        runTask(() -> { service.requireAdmin(); return true; }, ignored -> {
            authorized = true;
            findViewById(R.id.nv_root).setVisibility(View.VISIBLE);
            onAuthorized();
        }, error -> {
            toast(errorMessage(error));
            finish();
        });
    }

    protected abstract void onAuthorized();

    @Override protected void onPause() {
        active = false;
        authorized = false;
        generation++;
        findViewById(R.id.nv_root).setVisibility(View.INVISIBLE);
        super.onPause();
    }

    protected <T> void runTask(Callable<T> task, Consumer<T> success, Consumer<Exception> failure) {
        int expected = generation;
        AppExecutors.databaseExecutor().execute(() -> {
            T result;
            try {
                result = task.call();
            } catch (Exception exception) {
                AppExecutors.runOnMainThread(() -> {
                    if (isCurrent(expected)) failure.accept(exception);
                });
                return;
            }
            AppExecutors.runOnMainThread(() -> {
                if (isCurrent(expected)) success.accept(result);
            });
        });
    }

    private boolean isCurrent(int expected) {
        return active && expected == generation && !isFinishing() && !isDestroyed();
    }

    protected int errorMessage(Exception error) {
        return error instanceof EmployeeException
                ? ((EmployeeException) error).messageRes : R.string.nv_loi_du_lieu;
    }

    protected void showError(Exception error) {
        ((TextView) findViewById(R.id.nv_status)).setText(errorMessage(error));
    }

    protected void toast(int message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
