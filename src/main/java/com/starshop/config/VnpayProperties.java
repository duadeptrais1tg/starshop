package com.starshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình cổng VNPAY. tmnCode, hashSecret là bí mật: đọc từ biến môi trường VNPAY_TMN_CODE,
 * VNPAY_HASH_SECRET hoặc application-local.yml (không commit). Hướng dẫn: docs/thanh-toan-vnpay.md.
 *
 * @param payUrl        trang thanh toán (sandbox: https://sandbox.vnpayment.vn/paymentv2/vpcpay.html)
 * @param returnUrl     URL VNPAY chuyển trình duyệt về sau khi thanh toán
 * @param expireMinutes thời gian khách được phép thanh toán; quá hạn thì đơn tự hủy, hoàn tồn kho
 */
@ConfigurationProperties(prefix = "vnpay")
public record VnpayProperties(String tmnCode, String hashSecret, String payUrl, String returnUrl, int expireMinutes) {

    public VnpayProperties {
        if (expireMinutes <= 0) {
            expireMinutes = 15;
        }
    }

    public boolean isConfigured() {
        return hasText(tmnCode) && hasText(hashSecret) && hasText(payUrl) && hasText(returnUrl);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
