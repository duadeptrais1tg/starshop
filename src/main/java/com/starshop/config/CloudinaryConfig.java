package com.starshop.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Tạo bean Cloudinary dùng chung. Nếu chưa cấu hình key, ứng dụng vẫn khởi động được;
 * chỉ khi upload/xóa file mới báo lỗi (xem FileStorageServiceImpl).
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(CloudinaryProperties.class)
public class CloudinaryConfig {

    @Bean
    public Cloudinary cloudinary(CloudinaryProperties properties) {
        if (!properties.isConfigured()) {
            log.warn("Chưa cấu hình Cloudinary (CLOUDINARY_CLOUD_NAME / CLOUDINARY_API_KEY / CLOUDINARY_API_SECRET). "
                    + "Chức năng upload file sẽ không hoạt động.");
        }
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", properties.cloudName(),
                "api_key", properties.apiKey(),
                "api_secret", properties.apiSecret(),
                "secure", true));
    }
}
