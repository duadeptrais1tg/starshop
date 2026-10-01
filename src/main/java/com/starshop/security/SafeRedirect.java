package com.starshop.security;

/**
 * Chống "open redirect": tham số ?redirect= chỉ được là đường dẫn NỘI BỘ của StarShop.
 * Nếu không, kẻ xấu gửi link /auth/login?redirect=https://trang-gia.com để lừa người dùng
 * sang trang giả mạo ngay sau khi đăng nhập thật.
 */
public final class SafeRedirect {

    private static final int MAX_LENGTH = 500;

    private SafeRedirect() {
    }

    /** Trả về requested nếu an toàn, ngược lại trả về fallback. */
    public static String resolve(String requested, String fallback) {
        return isSafe(requested) ? requested : fallback;
    }

    static boolean isSafe(String path) {
        if (path == null || path.isEmpty() || path.length() > MAX_LENGTH) {
            return false;
        }
        // Ký tự điều khiển (tab, xuống dòng...): trình duyệt tự bỏ đi, "/\t/trang-gia.com" thành "//trang-gia.com"
        if (path.chars().anyMatch(c -> c < 0x20 || c == 0x7F)) {
            return false;
        }
        return path.startsWith("/")
                && !path.startsWith("//")          // //trang-gia.com = URL tuyệt đối tới trang khác
                && !path.startsWith("/\\")         // /\trang-gia.com: trình duyệt hiểu như //
                && !path.startsWith("/auth/");     // tránh vòng lặp quay lại trang đăng nhập/đăng xuất
    }
}
