package com.example.employeemanager.data.entities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bản ghi chấm công của một nhân viên trong một ngày.
 *
 * <p>Mỗi nhân viên chỉ có tối đa một bản ghi mỗi ngày nhờ chỉ mục UNIQUE
 * trên cặp ({@code employeeId}, {@code workDate}).</p>
 */
@Entity(
        tableName = "WorkLog",
        indices = {
                @Index(value = {"employeeId", "workDate"}, unique = true)
        },
        foreignKeys = {
                @ForeignKey(
                        entity = Employee.class,
                        parentColumns = "id",
                        childColumns = "employeeId",
                        onDelete = ForeignKey.RESTRICT
                )
        }
)
public class WorkLog {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "employeeId")
    public long employeeId;

    /**
     * Ngày làm việc theo múi giờ Việt Nam, định dạng {@code yyyy-MM-dd}.
     */
    @NonNull
    @ColumnInfo(name = "workDate")
    public String workDate;

    /**
     * Giờ chấm công vào, định dạng {@code HH:mm:ss}.
     */
    @NonNull
    @ColumnInfo(name = "checkInTime")
    public String checkInTime;

    /**
     * Giờ chấm công ra, định dạng {@code HH:mm:ss}; để trống khi phiên còn OPEN.
     */
    @Nullable
    @ColumnInfo(name = "checkOutTime")
    public String checkOutTime;

    /**
     * Số giờ làm việc; bằng 0 khi phiên còn OPEN.
     */
    @ColumnInfo(name = "totalHours")
    public double totalHours;

    /**
     * Trạng thái phiên chấm công: {@code OPEN} hoặc {@code COMPLETED}.
     */
    @NonNull
    @ColumnInfo(name = "status")
    public String status;

    public WorkLog() {
    }

    /**
     * Tạo phiên chấm công mới. Trường {@code id} do Room tự sinh nên không truyền vào đây.
     */
    @androidx.room.Ignore
    public WorkLog(long employeeId,
                   @NonNull String workDate,
                   @NonNull String checkInTime,
                   @Nullable String checkOutTime,
                   double totalHours,
                   @NonNull String status) {
        this.employeeId = employeeId;
        this.workDate = workDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.totalHours = totalHours;
        this.status = status;
    }
}
