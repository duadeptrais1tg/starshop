package com.starshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Cấu hình nghiệp vụ đơn hàng (khối app.order).
 *
 * @param returnDays số ngày sau khi giao khách được yêu cầu trả hàng
 */
@ConfigurationProperties(prefix = "app.order")
public record OrderProperties(int returnDays) {

    public OrderProperties {
        if (returnDays <= 0) {
            returnDays = 3;
        }
    }
}
