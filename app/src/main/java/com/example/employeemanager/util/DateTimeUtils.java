package com.example.employeemanager.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Tiện ích xử lý ngày giờ theo múi giờ nghiệp vụ Việt Nam.
 *
 * <p>Lớp này chỉ chứa hàm thuần, không truy cập cơ sở dữ liệu hay giao diện.</p>
 */
public final class DateTimeUtils {

    private static final ZoneId ZONE_ID = ZoneId.of(Constants.TIME_ZONE_ID);

    private static final DateTimeFormatter DATE_STORAGE_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_STORAGE_PATTERN);

    private static final DateTimeFormatter TIME_STORAGE_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.TIME_STORAGE_PATTERN);

    private static final DateTimeFormatter DATE_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.DATE_DISPLAY_PATTERN);

    private static final DateTimeFormatter TIME_DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern(Constants.TIME_DISPLAY_PATTERN);

    private DateTimeUtils() {
        // Lớp tiện ích không được khởi tạo.
    }

    /**
     * Trả về múi giờ nghiệp vụ cố định của ứng dụng.
     */
    public static ZoneId zoneId() {
        return ZONE_ID;
    }

    /**
     * Tạo đồng hồ hệ thống theo múi giờ nghiệp vụ.
     */
    public static Clock systemClock() {
        return Clock.system(ZONE_ID);
    }

    /**
     * Chuyển đồng hồ về múi giờ nghiệp vụ trước khi lấy ngày/giờ.
     */
    public static Clock withZone(Clock clock) {
        if (clock == null) {
            return systemClock();
        }
        return clock.withZone(ZONE_ID);
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.now(withZone(clock));
    }

    public static LocalTime nowTime(Clock clock) {
        return LocalTime.now(withZone(clock));
    }

    public static String todayString(Clock clock) {
        return today(clock).format(DATE_STORAGE_FORMATTER);
    }

    public static String nowTimeString(Clock clock) {
        return nowTime(clock).format(TIME_STORAGE_FORMATTER);
    }

    public static String monthKey(LocalDate date) {
        return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
    }

    public static String monthKey(int year, int month) {
        return String.format("%04d-%02d", year, month);
    }

    public static String formatDate(LocalDate date) {
        return date == null ? "" : date.format(DATE_DISPLAY_FORMATTER);
    }

    public static String formatDate(String storedDate) {
        if (storedDate == null || storedDate.trim().isEmpty()) {
            return "";
        }
        return formatDate(LocalDate.parse(storedDate, DATE_STORAGE_FORMATTER));
    }

    public static String formatTime(LocalTime time) {
        return time == null ? "" : time.format(TIME_DISPLAY_FORMATTER);
    }

    public static String formatTime(String storedTime) {
        if (storedTime == null || storedTime.trim().isEmpty()) {
            return "";
        }
        return formatTime(LocalTime.parse(storedTime, TIME_STORAGE_FORMATTER));
    }

    public static LocalDate parseDate(String storedDate) {
        if (storedDate == null || storedDate.trim().isEmpty()) {
            return null;
        }
        return LocalDate.parse(storedDate.trim(), DATE_STORAGE_FORMATTER);
    }

    public static LocalTime parseTime(String storedTime) {
        if (storedTime == null || storedTime.trim().isEmpty()) {
            return null;
        }
        return LocalTime.parse(storedTime.trim(), TIME_STORAGE_FORMATTER);
    }

    /**
     * Định dạng số giờ với tối đa hai chữ số thập phân để hiển thị.
     */
    public static String formatHours(double hours) {
        if (Double.isNaN(hours) || Double.isInfinite(hours)) {
            return "0";
        }
        String value = String.format(java.util.Locale.US, "%.2f", hours);
        if (value.endsWith(".00")) {
            return value.substring(0, value.length() - 3);
        }
        if (value.endsWith("0")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
