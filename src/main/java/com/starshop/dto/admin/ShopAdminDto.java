package com.starshop.dto.admin;

import com.starshop.entity.enums.ShopStatus;
import lombok.Builder;
import lombok.Getter;

/**
 * Thông tin shop hiển thị cho Admin.
 */
@Getter
@Builder
public class ShopAdminDto {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final String logoUrl;
    private final String pickupAddress;
    private final String phone;
    private final ShopStatus status;
    private final String statusReason;
    private final Long ownerId;
    private final String ownerName;
    private final String ownerEmail;
    private final Long storeId;
    private final String storeName;
    private final String createdAt;
}
