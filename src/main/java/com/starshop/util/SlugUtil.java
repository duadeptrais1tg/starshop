package com.starshop.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Tạo slug cho URL từ chuỗi tiếng Việt, ví dụ "Hoa Hồng Đỏ" -> "hoa-hong-do".
 */
public final class SlugUtil {

    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");
    private static final Pattern EDGE_DASHES = Pattern.compile("(^-+)|(-+$)");

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String text = input.trim().replace('đ', 'd').replace('Đ', 'D');
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        text = DIACRITICS.matcher(text).replaceAll("");
        text = NON_ALPHANUMERIC.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("-");
        return EDGE_DASHES.matcher(text).replaceAll("");
    }
}
