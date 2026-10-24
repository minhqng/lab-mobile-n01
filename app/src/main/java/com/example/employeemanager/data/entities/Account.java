package com.example.employeemanager.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tài khoản đăng nhập của ứng dụng.
 *
 * <p>Cột {@code password} chỉ lưu chuỗi băm có salt do module đăng nhập tạo ra,
 * tuyệt đối không lưu mật khẩu dạng rõ.</p>
 */
@Entity(
        tableName = "Account",
        indices = {
                @Index(value = {"username"}, unique = true)
        }
)
public class Account {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "username")
    public String username;

    @NonNull
    @ColumnInfo(name = "password")
    public String password;

    @NonNull
    @ColumnInfo(name = "role")
    public String role;

    public Account() {
    }

    /**
     * Tạo tài khoản mới. Trường {@code id} do Room tự sinh nên không truyền vào đây.
     */
    @androidx.room.Ignore
    public Account(@NonNull String username, @NonNull String password, @NonNull String role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    /**
     * Tạo đối tượng đầy đủ, chủ yếu dùng trong kiểm thử hoặc khi đọc dữ liệu có sẵn.
     */
    @androidx.room.Ignore
    public Account(long id, @NonNull String username, @NonNull String password, @NonNull String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
