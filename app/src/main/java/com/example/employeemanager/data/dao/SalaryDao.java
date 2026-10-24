package com.example.employeemanager.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.employeemanager.data.entities.Salary;

import java.util.List;

/**
 * Truy vấn bảng lương tháng.
 *
 * <p>Mọi phương thức phải được gọi trên luồng nền. Chỉ mục UNIQUE trên
 * ({@code employeeId}, {@code month}, {@code year}) bảo đảm không sinh bản ghi trùng.</p>
 */
@Dao
public interface SalaryDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Salary salary);

    @Update
    void update(Salary salary);

    @Delete
    void delete(Salary salary);

    @Query("SELECT * FROM Salary WHERE id = :salaryId LIMIT 1")
    Salary findById(long salaryId);

    @Query("SELECT * FROM Salary WHERE employeeId = :employeeId AND month = :month AND year = :year LIMIT 1")
    Salary findByEmployeeAndMonth(long employeeId, int month, int year);

    @Query("SELECT * FROM Salary WHERE month = :month AND year = :year ORDER BY employeeId ASC")
    List<Salary> findByMonth(int month, int year);

    @Query("SELECT * FROM Salary WHERE employeeId = :employeeId ORDER BY year DESC, month DESC")
    List<Salary> findByEmployeeId(long employeeId);

    @Query("SELECT COUNT(*) FROM Salary WHERE employeeId = :employeeId")
    long countByEmployeeId(long employeeId);
}
