package com.example.employeemanager.data.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.employeemanager.data.entities.Account;

import java.util.List;

/**
 * Truy vấn tài khoản đăng nhập.
 *
 * <p>Mọi phương thức phải được gọi trên luồng nền thông qua {@code AppExecutors}.</p>
 */
@Dao
public interface AccountDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insert(Account account);

    @Update
    void update(Account account);

    @Delete
    void delete(Account account);

    @Query("SELECT * FROM Account WHERE id = :accountId LIMIT 1")
    Account findById(long accountId);

    /**
     * Tìm tài khoản theo tên đăng nhập đã được chuẩn hóa (chữ thường, đã trim).
     */
    @Query("SELECT * FROM Account WHERE username = :username LIMIT 1")
    Account findByUsername(String username);

    @Query("SELECT * FROM Account ORDER BY username ASC")
    List<Account> getAll();

    @Query("SELECT COUNT(*) FROM Account")
    long count();
}
