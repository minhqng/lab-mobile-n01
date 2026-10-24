package com.example.employeemanager.employee;

import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.Employee;
import java.util.Locale;
import java.util.regex.Pattern;

/** Quy tắc dùng chung cho biểu mẫu và dịch vụ, không truy cập giao diện. */
public final class EmployeeValidator {
    private static final Pattern EMAIL = Pattern.compile(
            "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]*[A-Za-z0-9])?\\.)+"
            + "[A-Za-z]{2,63}");

    private EmployeeValidator() { }

    public static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    public static Employee validate(UpdateEmployeeRequest request) {
        if (request == null) throw new EmployeeException(null, R.string.nv_loi_du_lieu);
        String code = trim(request.code).toUpperCase(Locale.ROOT);
        String name = trim(request.fullname);
        String email = trim(request.email).toLowerCase(Locale.ROOT);
        String phone = trim(request.phone);
        if (code.isEmpty()) throw new EmployeeException("code", R.string.nv_loi_ma);
        int length = name.codePointCount(0, name.length());
        if (length < 2 || length > 100)
            throw new EmployeeException("fullname", R.string.nv_loi_ten);
        if (!EMAIL.matcher(email).matches())
            throw new EmployeeException("email", R.string.nv_loi_email);
        if (!phone.matches("0[0-9]{9}"))
            throw new EmployeeException("phone", R.string.nv_loi_dien_thoai);
        long rate;
        try {
            String input = trim(request.hourlyRate);
            if (!input.matches("[0-9]+")) throw new NumberFormatException();
            rate = Long.parseLong(input);
            if (rate <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            throw new EmployeeException("hourlyRate", R.string.nv_loi_don_gia);
        }
        return new Employee(code, name, email, phone, rate, 0);
    }

    public static String validateAccount(CreateEmployeeRequest request) {
        String username = trim(request.username).toLowerCase(Locale.ROOT);
        if (username.isEmpty())
            throw new EmployeeException("username", R.string.nv_loi_tai_khoan);
        if (trim(request.password).isEmpty())
            throw new EmployeeException("password", R.string.nv_loi_mat_khau);
        return username;
    }
}
