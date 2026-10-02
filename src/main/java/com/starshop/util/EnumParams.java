package com.starshop.util;

import java.util.Locale;

/**
 * Đọc tham số URL thành enum; giá trị rỗng hoặc sai (người dùng tự sửa URL) thì coi như không lọc.
 */
public final class EnumParams {

    private EnumParams() {
    }

    public static <E extends Enum<E>> E parse(Class<E> type, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
