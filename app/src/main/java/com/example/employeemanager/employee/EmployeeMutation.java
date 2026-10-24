package com.example.employeemanager.employee;

import androidx.lifecycle.ViewModel;
import com.example.employeemanager.util.AppExecutors;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Giữ kết quả ghi qua xoay màn hình, không giữ Activity khi màn hình tạm dừng. */
public final class EmployeeMutation extends ViewModel {
    private boolean running, completed;
    private Exception error;
    private Consumer<Exception> listener;

    public boolean isRunning() { return running; }

    public boolean attach(Consumer<Exception> callback) {
        listener = callback;
        boolean delivered = completed;
        dispatch();
        return delivered;
    }

    public void detach() { listener = null; }

    public void start(Callable<?> action) {
        if (running || completed) return;
        running = true;
        AppExecutors.databaseExecutor().execute(() -> {
            Exception failure = null;
            try { action.call(); } catch (Exception exception) { failure = exception; }
            Exception result = failure;
            AppExecutors.runOnMainThread(() -> {
                running = false;
                completed = true;
                error = result;
                dispatch();
            });
        });
    }

    private void dispatch() {
        if (!completed || listener == null) return;
        Exception result = error;
        completed = false;
        error = null;
        listener.accept(result);
    }

    @Override protected void onCleared() { detach(); }
}
