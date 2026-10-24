package com.example.employeemanager.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.employeemanager.data.entities.WorkLog;

import java.util.List;

/**
 * Truy vấn bản ghi chấm công.
 *
 * <p>Tham số {@code monthKey} dùng định dạng {@code yyyy-MM}, ví dụ {@code 2026-10}.
 * Mọi phương thức phải được gọi trên luồng nền.</p>
 */
@Dao
public interface WorkLogDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(WorkLog workLog);

    @Update
    void update(WorkLog workLog);

    @Delete
    void delete(WorkLog workLog);

    @Query("SELECT * FROM WorkLog WHERE id = :workLogId LIMIT 1")
    WorkLog findById(long workLogId);

    @Query("SELECT * FROM WorkLog WHERE employeeId = :employeeId AND workDate = :workDate LIMIT 1")
    WorkLog findByEmployeeAndDate(long employeeId, String workDate);

    /**
     * Tìm phiên chấm công còn OPEN của nhân viên, không giới hạn ngày.
     * Dùng để chặn mở phiên mới khi còn bản ghi chưa hoàn tất.
     */
    @Query("SELECT * FROM WorkLog WHERE employeeId = :employeeId AND status = 'OPEN' LIMIT 1")
    WorkLog findOpenByEmployee(long employeeId);

    @Query("SELECT * FROM WorkLog WHERE employeeId = :employeeId AND status = 'OPEN' "
            + "AND substr(workDate, 1, 7) = :monthKey LIMIT 1")
    WorkLog findOpenByEmployeeAndMonth(long employeeId, String monthKey);

    @Query("SELECT * FROM WorkLog WHERE employeeId = :employeeId "
            + "AND substr(workDate, 1, 7) = :monthKey ORDER BY workDate ASC")
    List<WorkLog> findByEmployeeAndMonth(long employeeId, String monthKey);

    @Query("SELECT * FROM WorkLog WHERE employeeId = :employeeId AND status = 'COMPLETED' "
            + "AND substr(workDate, 1, 7) = :monthKey ORDER BY workDate ASC")
    List<WorkLog> findCompletedByEmployeeAndMonth(long employeeId, String monthKey);

    @Query("SELECT COUNT(*) FROM WorkLog WHERE employeeId = :employeeId")
    long countByEmployeeId(long employeeId);
}
