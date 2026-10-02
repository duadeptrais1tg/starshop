package com.starshop.repository.spec;

import java.util.Locale;

/**
 * Tiện ích chung cho JPA Specification.
 */
public final class SpecUtils {

    /** Ký tự escape dùng trong LIKE. */
    public static final char ESCAPE = '\\';

    private SpecUtils() {
    }

    /**
     * Chuỗi "%từ khóa%" chữ thường; escape % và _ để người dùng gõ "50%" không bị hiểu là ký tự đại diện.
     */
    public static String likePattern(String keyword) {
        String escaped = keyword.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
