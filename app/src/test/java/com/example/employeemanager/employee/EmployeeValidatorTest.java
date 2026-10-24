package com.example.employeemanager.employee;

import static org.junit.Assert.*;
import com.example.employeemanager.R;
import com.example.employeemanager.data.entities.Employee;
import org.junit.Test;
import java.util.Locale;

/** Kiểm tra quy tắc nhập liệu độc lập với Android và cơ sở dữ liệu. */
public class EmployeeValidatorTest {
    private UpdateEmployeeRequest request(String code, String name, String email, String phone, String rate) {
        return new UpdateEmployeeRequest(code, name, email, phone, rate);
    }

    private void invalid(String field, UpdateEmployeeRequest request) {
        assertEquals("Lỗi phải gắn đúng trường", field,
                assertThrows(EmployeeException.class, () -> EmployeeValidator.validate(request)).field);
    }

    @Test public void normalizesWithoutRemovingVietnameseAccents() {
        Employee employee = EmployeeValidator.validate(request(" nv001 ", " Nguyễn Ánh ",
                " ANH@EXAMPLE.COM ", " 0912345678 ", " 50000 "));
        assertEquals("NV001", employee.code);
        assertEquals("Nguyễn Ánh", employee.fullname);
        assertEquals("anh@example.com", employee.email);
        assertEquals("0912345678", employee.phone);
        assertEquals(50000, employee.hourlyRate);
    }

    @Test public void rejectsEmptyCode() { invalid("code", request(" ", "An", "a@b.vn", "0912345678", "1")); }
    @Test public void rejectsNullRequest() { assertThrows(EmployeeException.class, () -> EmployeeValidator.validate(null)); }
    @Test public void rejectsShortName() { invalid("fullname", request("N", "A", "a@b.vn", "0912345678", "1")); }
    @Test public void rejectsLongName() { invalid("fullname", request("N", "a".repeat(101), "a@b.vn", "0912345678", "1")); }
    @Test public void acceptsNameLengthBoundaries() {
        EmployeeValidator.validate(request("N", "An", "a@b.vn", "0912345678", "1"));
        EmployeeValidator.validate(request("N", "a".repeat(100), "a@b.vn", "0912345678", "1"));
    }
    @Test public void rejectsInvalidEmails() {
        for (String email : new String[]{"", "a", "a@b", "a b@c.vn", "a@@b.vn", "a..b@c.vn", "a@-b.vn"})
            invalid("email", request("N", "An", email, "0912345678", "1"));
    }
    @Test public void rejectsInvalidPhones() {
        for (String phone : new String[]{"", "9123456789", "091234567", "09123456789", "091 234567", "091234567a", "+84912345678"})
            invalid("phone", request("N", "An", "a@b.vn", phone, "1"));
    }
    @Test public void rejectsZeroRate() { invalid("hourlyRate", request("N", "An", "a@b.vn", "0912345678", "0")); }
    @Test public void rejectsNegativeRate() { invalid("hourlyRate", request("N", "An", "a@b.vn", "0912345678", "-1")); }
    @Test public void rejectsNonIntegerRate() {
        for (String rate : new String[]{"", "abc", "1.5", "50.000", "+1", "1 000"})
            invalid("hourlyRate", request("N", "An", "a@b.vn", "0912345678", rate));
    }
    @Test public void rejectsOverflow() { invalid("hourlyRate", request("N", "An", "a@b.vn", "0912345678", "9223372036854775808")); }
    @Test public void acceptsLongMaximum() {
        assertEquals(Long.MAX_VALUE, EmployeeValidator.validate(request("N", "An", "a@b.vn", "0912345678", Long.toString(Long.MAX_VALUE))).hourlyRate);
    }
    @Test public void normalizesUsernameUsingRootLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals("id", EmployeeValidator.validateAccount(new CreateEmployeeRequest("N", "An", "a@b.vn", "0912345678", "1", " ID ", "không rỗng")));
            assertEquals("ID", EmployeeValidator.validate(request(" id ", "An", "a@b.vn", "0912345678", "1")).code);
        } finally { Locale.setDefault(previous); }
    }
    @Test public void rejectsEmptyUsername() {
        assertEquals("username", assertThrows(EmployeeException.class, () -> EmployeeValidator.validateAccount(
                new CreateEmployeeRequest("N", "An", "a@b.vn", "0912345678", "1", " ", "không rỗng"))).field);
    }
    @Test public void rejectsEmptyPassword() {
        assertEquals(R.string.nv_loi_mat_khau, assertThrows(EmployeeException.class, () -> EmployeeValidator.validateAccount(
                new CreateEmployeeRequest("N", "An", "a@b.vn", "0912345678", "1", "nv", " "))).messageRes);
    }
}
