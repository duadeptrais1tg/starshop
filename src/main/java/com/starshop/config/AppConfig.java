package com.starshop.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Clock;

/**
 * Cấu hình chung:
 * - @EnableAsync: cho phép @Async (gửi email không làm chậm request), dùng thread pool mặc định của Spring Boot.
 * - AppMailProperties: cấu hình email (khối app.mail).
 * - Clock: nguồn thời gian dùng chung, test có thể thay bằng Clock cố định.
 */
@Configuration
@EnableAsync
@EnableConfigurationProperties(AppMailProperties.class)
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
