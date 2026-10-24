package com.example.employeemanager.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.employeemanager.data.entities.Employee;

import java.util.List;

/**
 * Truy vấn nhân viên.
 *
 * <p>Mọi phương thức phải được gọi trên luồng nền. Các ràng buộc trùng mã, email và
 * tài khoản được bảo vệ ở tầng cơ sở dữ liệu bằng chỉ mục UNIQUE.</p>
 */
@Dao
public interface EmployeeDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Employee employee);

    @Update
    void update(Employee employee);

    @Delete
    void delete(Employee employee);

    @Query("SELECT * FROM Employee WHERE id = :employeeId LIMIT 1")
    Employee findById(long employeeId);

    @Query("SELECT * FROM Employee WHERE code = :code LIMIT 1")
    Employee findByCode(String code);

    @Query("SELECT * FROM Employee WHERE email = :email LIMIT 1")
    Employee findByEmail(String email);

    @Query("SELECT * FROM Employee WHERE accountId = :accountId LIMIT 1")
    Employee findByAccountId(long accountId);

    @Query("SELECT * FROM Employee ORDER BY fullname COLLATE NOCASE ASC, code ASC")
    List<Employee> getAll();

    /**
     * Tìm gần đúng theo mã, họ tên hoặc email; không phân biệt hoa thường với ký tự ASCII.
     */
    @Query("SELECT * FROM Employee "
            + "WHERE code LIKE '%' || :keyword || '%' "
            + "OR fullname LIKE '%' || :keyword || '%' "
            + "OR email LIKE '%' || :keyword || '%' "
            + "ORDER BY fullname COLLATE NOCASE ASC, code ASC")
    List<Employee> search(String keyword);

    @Query("SELECT COUNT(*) FROM Employee")
    long count();
}
