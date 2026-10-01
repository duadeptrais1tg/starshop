package com.starshop.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Cấu hình JWT, đọc từ khối "jwt" trong application.yml.
 *
 * @param secret       khóa ký HMAC, tối thiểu 32 ký tự (secret càng dài, thuật toán càng mạnh: HS256/HS384/HS512), lấy từ biến môi trường JWT_SECRET
 * @param expiration   thời gian sống của token, ví dụ 8h, 30m
 * @param cookieSecure true khi chạy HTTPS (cookie chỉ gửi qua kết nối an toàn)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration expiration, boolean cookieSecure) {
}
