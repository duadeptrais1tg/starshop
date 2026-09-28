package com.starshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Thông tin kết nối Cloudinary, đọc từ khối "cloudinary" trong application.yml
 * (giá trị thật lấy từ biến môi trường, không commit).
 *
 * @param cloudName tên cloud (Dashboard Cloudinary)
 * @param apiKey    API key
 * @param apiSecret API secret
 * @param folder    thư mục gốc chứa mọi file của StarShop, ví dụ "starshop"
 */
@ConfigurationProperties(prefix = "cloudinary")
public record CloudinaryProperties(String cloudName, String apiKey, String apiSecret, String folder) {

    public boolean isConfigured() {
        return hasText(cloudName) && hasText(apiKey) && hasText(apiSecret);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
