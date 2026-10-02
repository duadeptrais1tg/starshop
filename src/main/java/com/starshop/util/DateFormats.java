package com.starshop.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Định dạng ngày giờ hiển thị trên giao diện (JSTL fmt:formatDate không hỗ trợ LocalDateTime).
 */
public final class DateFormats {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateFormats() {
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "" : value.format(DATE_TIME);
    }
}
