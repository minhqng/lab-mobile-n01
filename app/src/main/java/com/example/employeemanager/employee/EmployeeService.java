package com.example.employeemanager.employee;

import android.database.sqlite.SQLiteConstraintException;
import androidx.annotation.WorkerThread;
import com.example.employeemanager.R;
import com.example.employeemanager.data.AppDB;
import com.example.employeemanager.data.entities.Account;
import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.util.Constants;
import java.util.List;
import java.util.Objects;

/** Nghiệp vụ nhân viên đồng bộ trên luồng nền, độc lập với Activity. */
@WorkerThread
public final class EmployeeService {
    private final AppDB database;
    private final EmployeeAuth auth;

    public EmployeeService(AppDB database, EmployeeAuth auth) {
        this.database = Objects.requireNonNull(database);
        this.auth = Objects.requireNonNull(auth);
    }

    private Account currentAccount() {
        long id = auth.currentAccountId();
        Account account = id > 0 ? database.accountDao().findById(id) : null;
        if (account == null) throw new EmployeeException(null, R.string.nv_khong_co_quyen);
        return account;
    }

    public void requireAdmin() {
        if (!Constants.ROLE_ADMIN.equals(currentAccount().role))
            throw new EmployeeException(null, R.string.nv_khong_co_quyen);
    }

    private void requireOwnerOrAdmin(long accountId) {
        Account current = currentAccount();
        if (!Constants.ROLE_ADMIN.equals(current.role)
                && !(Constants.ROLE_EMPLOYEE.equals(current.role) && current.id == accountId))
            throw new EmployeeException(null, R.string.nv_khong_co_quyen);
    }

    private Employee findRequired(long id) {
        Employee employee = id > 0 ? database.employeeDao().findById(id) : null;
        if (employee == null) throw new EmployeeException(null, R.string.nv_khong_ton_tai);
        return employee;
    }

    private void checkDuplicates(Employee employee, long ownId) {
        Employee byCode = database.employeeDao().findByCode(employee.code);
        if (byCode != null && byCode.id != ownId)
            throw new EmployeeException("code", R.string.nv_trung_ma);
        Employee byEmail = database.employeeDao().findByEmail(employee.email);
        if (byEmail != null && byEmail.id != ownId)
            throw new EmployeeException("email", R.string.nv_trung_email);
    }

    public Employee createEmployee(CreateEmployeeRequest request) {
        requireAdmin();
        Employee employee = EmployeeValidator.validate(request);
        String username = EmployeeValidator.validateAccount(request);
        checkDuplicates(employee, 0);
        if (database.accountDao().findByUsername(username) != null)
            throw new EmployeeException("username", R.string.nv_trung_tai_khoan);
        String hash = auth.hashPassword(request.password);
        if (hash == null || hash.trim().isEmpty() || hash.equals(request.password))
            throw new EmployeeException(null, R.string.nv_auth_chua_san_sang);
        try {
            return database.runInTransaction(() -> {
                requireAdmin();
                checkDuplicates(employee, 0);
                if (database.accountDao().findByUsername(username) != null)
                    throw new EmployeeException("username", R.string.nv_trung_tai_khoan);
                employee.accountId = database.accountDao().insert(
                        new Account(username, hash, Constants.ROLE_EMPLOYEE));
                employee.id = database.employeeDao().insert(employee);
                return employee;
            });
        } catch (SQLiteConstraintException exception) {
            throw new EmployeeException(null, R.string.nv_loi_luu);
        }
    }

    public Employee updateEmployee(long employeeId, UpdateEmployeeRequest request) {
        requireAdmin();
        Employee changed = EmployeeValidator.validate(request);
        try {
            return database.runInTransaction(() -> {
                requireAdmin();
                Employee original = findRequired(employeeId);
                checkDuplicates(changed, employeeId);
                changed.id = original.id;
                changed.accountId = original.accountId;
                database.employeeDao().update(changed);
                return changed;
            });
        } catch (SQLiteConstraintException exception) {
            throw new EmployeeException(null, R.string.nv_loi_luu);
        }
    }

    public void deleteEmployee(long employeeId) {
        requireAdmin();
        try {
            database.runInTransaction(() -> {
                requireAdmin();
                Employee employee = findRequired(employeeId);
                if (database.workLogDao().countByEmployeeId(employeeId) > 0
                        || database.salaryDao().countByEmployeeId(employeeId) > 0)
                    throw new EmployeeException(null, R.string.nv_co_lich_su);
                Account account = database.accountDao().findById(employee.accountId);
                if (account == null || !Constants.ROLE_EMPLOYEE.equals(account.role))
                    throw new EmployeeException(null, R.string.nv_loi_lien_ket);
                database.employeeDao().delete(employee);
                database.accountDao().delete(account);
            });
        } catch (SQLiteConstraintException exception) {
            throw new EmployeeException(null, R.string.nv_loi_xoa);
        }
    }

    public List<Employee> searchEmployees(String keyword) {
        requireAdmin();
        return database.employeeDao().search(keyword);
    }

    public Employee getEmployeeById(long employeeId) {
        Account current = currentAccount();
        if (Constants.ROLE_ADMIN.equals(current.role)) return findRequired(employeeId);
        if (Constants.ROLE_EMPLOYEE.equals(current.role)) {
            Employee own = database.employeeDao().findByAccountId(current.id);
            if (own != null && own.id == employeeId) return own;
        }
        throw new EmployeeException(null, R.string.nv_khong_co_quyen);
    }

    public Employee getEmployeeByAccountId(long accountId) {
        requireOwnerOrAdmin(accountId);
        Employee employee = database.employeeDao().findByAccountId(accountId);
        if (employee == null) throw new EmployeeException(null, R.string.nv_khong_ton_tai);
        return employee;
    }
}
