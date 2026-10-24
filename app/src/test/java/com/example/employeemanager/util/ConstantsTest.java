package com.example.employeemanager.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZoneId;

/**
 * Kiểm tra các giá trị nền tảng dùng chung giữa các module.
 */
public class ConstantsTest {

    @Test
    public void rolesMatchSpecification() {
        assertEquals("ADMIN", Constants.ROLE_ADMIN);
        assertEquals("EMPLOYEE", Constants.ROLE_EMPLOYEE);
    }

    @Test
    public void statusesMatchSpecification() {
        assertEquals("OPEN", Constants.STATUS_OPEN);
        assertEquals("COMPLETED", Constants.STATUS_COMPLETED);
        assertEquals("DRAFT", Constants.STATUS_DRAFT);
        assertEquals("PAID", Constants.STATUS_PAID);
    }

    @Test
    public void timeZoneIsVietnam() {
        assertEquals("Asia/Ho_Chi_Minh", Constants.TIME_ZONE_ID);
        assertEquals(ZoneId.of("Asia/Ho_Chi_Minh"), ZoneId.of(Constants.TIME_ZONE_ID));
    }

    @Test
    public void payrollRulesMatchSpecification() {
        assertEquals(480, Constants.STANDARD_MINUTES_PER_DAY);
        assertEquals(1.5, Constants.OVERTIME_MULTIPLIER, 0.0001);
        assertTrue("Đơn vị tiền phải là VND", "VND".equals(Constants.CURRENCY_UNIT));
    }

    @Test
    public void storageFormatsMatchSpecification() {
        assertEquals("yyyy-MM-dd", Constants.DATE_STORAGE_PATTERN);
        assertEquals("HH:mm:ss", Constants.TIME_STORAGE_PATTERN);
        assertEquals("dd/MM/yyyy", Constants.DATE_DISPLAY_PATTERN);
        assertEquals("HH:mm", Constants.TIME_DISPLAY_PATTERN);
    }
}
