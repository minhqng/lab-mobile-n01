package com.example.employeemanager.util;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Bộ thực thi dùng chung cho các thao tác cơ sở dữ liệu và cập nhật giao diện.
 *
 * <p>Luồng nền có kích thước cố định, được đặt tên để dễ nhận diện khi gỡ lỗi và là
 * luồng daemon. Không dùng lớp này để chạy tác vụ dài vô hạn trên luồng giao diện.</p>
 */
public final class AppExecutors {

    private static final int DATABASE_THREAD_COUNT = 3;

    private static final ExecutorService DATABASE_EXECUTOR =
            Executors.newFixedThreadPool(DATABASE_THREAD_COUNT, new DatabaseThreadFactory());

    private static final Handler MAIN_THREAD_HANDLER = new Handler(Looper.getMainLooper());

    private AppExecutors() {
        // Lớp tiện ích không được khởi tạo.
    }

    /**
     * Trả về bộ thực thi dùng chung cho truy vấn và ghi cơ sở dữ liệu.
     */
    public static ExecutorService databaseExecutor() {
        return DATABASE_EXECUTOR;
    }

    /**
     * Đưa một tác vụ cập nhật giao diện về luồng chính.
     */
    public static void runOnMainThread(Runnable action) {
        MAIN_THREAD_HANDLER.post(action);
    }

    /**
     * Giải phóng luồng nền khi ứng dụng kết thúc hoặc khi kiểm thử cần dọn tài nguyên.
     */
    public static void shutdown() {
        DATABASE_EXECUTOR.shutdown();
    }

    private static final class DatabaseThreadFactory implements ThreadFactory {

        private final AtomicInteger threadCounter = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "employee-db-" + threadCounter.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
