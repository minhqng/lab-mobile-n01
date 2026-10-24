package com.example.employeemanager.data.dto;

import com.example.employeemanager.data.entities.Employee;
import com.example.employeemanager.data.entities.Salary;

import java.util.Objects;

/**
 * Đối tượng truyền dữ liệu (DTO) chứa thông tin chi tiết bảng lương kèm nhân viên liên kết.
 *
 * <p>DTO này dùng để hiển thị dữ liệu chi tiết cho quản trị viên, không phải là một Entity Room.</p>
 */
public class SalaryDetailDto {

    private final Salary salary;
    private final Employee employee;

    public SalaryDetailDto(Salary salary, Employee employee) {
        this.salary = salary;
        this.employee = employee;
    }

    /**
     * Thông tin bảng lương.
     */
    public Salary getSalary() {
        return salary;
    }

    /**
     * Thông tin nhân viên nhận lương (có thể null nếu nhân viên không còn tồn tại).
     */
    public Employee getEmployee() {
        return employee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SalaryDetailDto that = (SalaryDetailDto) o;
        return Objects.equals(salary, that.salary) && Objects.equals(employee, that.employee);
    }

    @Override
    public int hashCode() {
        return Objects.hash(salary, employee);
    }

    @Override
    public String toString() {
        return "SalaryDetailDto{"
                + "salary=" + salary
                + ", employee=" + employee
                + '}';
    }
}
