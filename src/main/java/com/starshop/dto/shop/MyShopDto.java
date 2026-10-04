package com.starshop.dto.shop;

import com.starshop.entity.enums.ShopStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Shop của chính người dùng (trang "Mở shop" và trang quản lý shop của vendor).
 */
@Getter
@Builder
public class MyShopDto {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final String logoUrl;
    private final String bannerUrl;
    private final String pickupAddress;
    private final String phone;
    private final ShopStatus status;
    private final String statusReason;
    private final String storeName;
    private final String createdAt;

    public boolean isPending() {
        return status == ShopStatus.PENDING;
    }

    public boolean isRejected() {
        return status == ShopStatus.REJECTED;
    }

    /** Đã được duyệt (kể cả đang bị đình chỉ): quản lý ở kênh người bán. */
    public boolean isApproved() {
        return status == ShopStatus.APPROVED || status == ShopStatus.SUSPENDED;
    }
}
