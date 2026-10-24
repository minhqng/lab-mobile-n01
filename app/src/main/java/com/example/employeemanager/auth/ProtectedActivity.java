package com.example.employeemanager.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.example.employeemanager.MainActivity;
import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.util.AppExecutors;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Xác minh lại phiên khi trở về màn hình và bỏ kết quả của vòng đời cũ. */
public abstract class ProtectedActivity extends AppCompatActivity {
    protected SessionManager sessions;
    private boolean active;
    private int generation;
    protected abstract String requiredRole();
    protected void onSessionReady() { }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        sessions = new SessionManager(this, AppDB.getInstance(this));
    }
    @Override public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        View root = ((android.view.ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        int left = root.getPaddingLeft(), top = root.getPaddingTop();
        int right = root.getPaddingRight(), bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return insets;
        });
    }
    @Override protected void onResume() {
        super.onResume();
        active = true;
        findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
        runTask(() -> { sessions.requireRole(requiredRole()); return true; }, value -> {
            findViewById(android.R.id.content).setVisibility(View.VISIBLE);
            onSessionReady();
        }, error -> redirect());
    }
    @Override protected void onPause() {
        active = false;
        generation++;
        findViewById(android.R.id.content).setVisibility(View.INVISIBLE);
        super.onPause();
    }
    protected void redirect() {
        startActivity(new Intent(this, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
    protected void logout() {
        runTask(() -> { sessions.logout(); return true; }, value -> redirect(), error -> redirect());
    }
    protected <T> void runTask(Callable<T> action, Consumer<T> success, Consumer<Exception> failure) {
        int expected = generation;
        AppExecutors.databaseExecutor().execute(() -> {
            try {
                T value = action.call();
                AppExecutors.runOnMainThread(() -> {
                    if (current(expected)) success.accept(value);
                });
            } catch (Exception error) {
                AppExecutors.runOnMainThread(() -> {
                    if (current(expected)) failure.accept(error);
                });
            }
        });
    }
    private boolean current(int expected) {
        return active && generation == expected && !isFinishing() && !isDestroyed();
    }
    protected String errorText(Exception error) {
        return getString(error instanceof SecurityException ? R.string.bc_permission_error : R.string.bc_data_error);
    }
}
