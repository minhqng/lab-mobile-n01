package com.example.employeemanager.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Thông tin nhân viên, gắn với đúng một tài khoản đăng nhập vai trò EMPLOYEE.
 *
 * <p>Quan hệ với {@link Account} dùng chế độ xóa RESTRICT để không làm mất dữ liệu
 * nhân viên khi tài khoản còn tồn tại.</p>
 */
@Entity(
        tableName = "Employee",
        indices = {
                @Index(value = {"code"}, unique = true),
                @Index(value = {"email"}, unique = true),
                @Index(value = {"accountId"}, unique = true)
        },
        foreignKeys = {
                @ForeignKey(
                        entity = Account.class,
                        parentColumns = "id",
                        childColumns = "accountId",
                        onDelete = ForeignKey.RESTRICT
                )
        }
)
public class Employee {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @NonNull
    @ColumnInfo(name = "code")
    public String code;

    @NonNull
    @ColumnInfo(name = "fullname")
    public String fullname;

    @NonNull
    @ColumnInfo(name = "email")
    public String email;

    @NonNull
    @ColumnInfo(name = "phone")
    public String phone;

    @ColumnInfo(name = "hourlyRate")
    public long hourlyRate;

    @ColumnInfo(name = "accountId")
    public long accountId;

    public Employee() {
    }

    /**
     * Tạo nhân viên mới. Trường {@code id} do Room tự sinh nên không truyền vào đây.
     */
    @androidx.room.Ignore
    public Employee(@NonNull String code,
                    @NonNull String fullname,
                    @NonNull String email,
                    @NonNull String phone,
                    long hourlyRate,
                    long accountId) {
        this.code = code;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.hourlyRate = hourlyRate;
        this.accountId = accountId;
    }

    /**
     * Tạo đối tượng đầy đủ, chủ yếu dùng trong kiểm thử hoặc khi đọc dữ liệu có sẵn.
     */
    @androidx.room.Ignore
    public Employee(long id,
                    @NonNull String code,
                    @NonNull String fullname,
                    @NonNull String email,
                    @NonNull String phone,
                    long hourlyRate,
                    long accountId) {
        this.id = id;
        this.code = code;
        this.fullname = fullname;
        this.email = email;
        this.phone = phone;
        this.hourlyRate = hourlyRate;
        this.accountId = accountId;
    }
}
