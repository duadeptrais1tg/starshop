package com.starshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình email riêng của StarShop (khối "app.mail"); thông tin SMTP nằm ở "spring.mail".
 *
 * @param from      địa chỉ người gửi (mặc định = tài khoản SMTP)
 * @param fromName  tên hiển thị người gửi
 * @param devLogOtp CHỈ dùng khi dev: chưa cấu hình SMTP thì in mã OTP ra log để test
 */
@ConfigurationProperties(prefix = "app.mail")
public record AppMailProperties(String from, String fromName, boolean devLogOtp) {
}
