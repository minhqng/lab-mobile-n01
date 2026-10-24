package com.example.employeemanager.auth;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.employeemanager.util.AppExecutors;

/** Giữ tác vụ đăng nhập qua xoay màn hình, không giữ tham chiếu Activity. */
public final class LoginState extends ViewModel {
    public final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    public final MutableLiveData<AuthResult> result = new MutableLiveData<>();
    public void login(SessionManager sessions, String username, String password, String errorMessage) {
        if (Boolean.TRUE.equals(loading.getValue())) return;
        loading.setValue(true);
        AppExecutors.databaseExecutor().execute(() -> {
            AuthResult value;
            try { value = sessions.login(username, password); }
            catch (Exception error) { value = AuthResult.failure(errorMessage); }
            AuthResult completed = value;
            AppExecutors.runOnMainThread(() -> {
                loading.setValue(false);
                result.setValue(completed);
            });
        });
    }
}
