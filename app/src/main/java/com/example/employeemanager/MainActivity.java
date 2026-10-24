package com.example.employeemanager;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Màn hình khởi động tối thiểu của giai đoạn nền tảng.
 *
 * <p>Màn hình này chỉ chứng minh dự án có thể biên dịch và khởi chạy. Khi module đăng nhập
 * hoàn thành, màn hình sẽ được thay bằng bước kiểm tra phiên và điều hướng theo vai trò.</p>
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
}
