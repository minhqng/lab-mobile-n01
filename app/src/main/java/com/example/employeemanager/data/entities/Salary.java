package com.example.employeemanager.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Bảng lương tháng của một nhân viên.
 *
 * <p>Mỗi nhân viên chỉ có tối đa một bảng lương trong một tháng/năm nhờ chỉ mục UNIQUE
 * trên bộ ba ({@code employeeId}, {@code month}, {@code year}).</p>
 */
@Entity(
        tableName = "Salary",
        indices = {
                @Index(value = {"employeeId", "month", "year"}, unique = true)
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
public class Salary {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "employeeId")
    public long employeeId;

    /**
     * Tháng tính lương, giá trị từ 1 đến 12.
     */
    @ColumnInfo(name = "month")
    public int month;

    @ColumnInfo(name = "year")
    public int year;

    /**
     * Tổng số giờ làm việc trong tháng, đã bao gồm giờ tăng ca.
     */
    @ColumnInfo(name = "totalHours")
    public double totalHours;

    /**
     * Số giờ tăng ca, đã nằm trong {@link #totalHours}.
     */
    @ColumnInfo(name = "overtimeHours")
    public double overtimeHours;

    /**
     * Tổng tiền lương theo đơn vị VND, làm tròn tới đồng.
     */
    @ColumnInfo(name = "totalSalary")
    public long totalSalary;

    /**
     * Trạng thái bảng lương: {@code DRAFT} hoặc {@code PAID}.
     */
    @NonNull
    @ColumnInfo(name = "status")
    public String status;

    public Salary() {
    }

    /**
     * Tạo bảng lương mới. Trường {@code id} do Room tự sinh nên không truyền vào đây.
     */
    @androidx.room.Ignore
    public Salary(long employeeId,
                  int month,
                  int year,
                  double totalHours,
                  double overtimeHours,
                  long totalSalary,
                  @NonNull String status) {
        this.employeeId = employeeId;
        this.month = month;
        this.year = year;
        this.totalHours = totalHours;
        this.overtimeHours = overtimeHours;
        this.totalSalary = totalSalary;
        this.status = status;
    }
}
