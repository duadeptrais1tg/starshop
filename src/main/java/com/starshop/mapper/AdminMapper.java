package com.starshop.mapper;

import com.starshop.dto.admin.ShopAdminDto;
import com.starshop.dto.admin.UserAdminDto;
import com.starshop.entity.Role;
import com.starshop.entity.Shop;
import com.starshop.entity.User;
import com.starshop.util.DateFormats;

import java.util.Comparator;

/**
 * Chuyển entity sang DTO cho các trang quản trị. Gọi trong transaction của service
 * (vì cần đọc quan hệ lazy như store, carrier, owner).
 */
public final class AdminMapper {

    private AdminMapper() {
    }

    public static UserAdminDto toUserDto(User user) {
        return UserAdminDto.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .roles(user.getRoles().stream().map(Role::getName).sorted(Comparator.naturalOrder()).toList())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .storeId(user.getStore() == null ? null : user.getStore().getId())
                .storeName(user.getStore() == null ? null : user.getStore().getName())
                .carrierId(user.getCarrier() == null ? null : user.getCarrier().getId())
                .carrierName(user.getCarrier() == null ? null : user.getCarrier().getName())
                .createdAt(DateFormats.dateTime(user.getCreatedAt()))
                .build();
    }

    public static ShopAdminDto toShopDto(Shop shop) {
        return ShopAdminDto.builder()
                .id(shop.getId())
                .name(shop.getName())
                .slug(shop.getSlug())
                .description(shop.getDescription())
                .logoUrl(shop.getLogoUrl())
                .pickupAddress(shop.getPickupAddress())
                .phone(shop.getPhone())
                .status(shop.getStatus())
                .statusReason(shop.getStatusReason())
                .ownerId(shop.getOwner().getId())
                .ownerName(shop.getOwner().getFullName())
                .ownerEmail(shop.getOwner().getEmail())
                .storeId(shop.getStore() == null ? null : shop.getStore().getId())
                .storeName(shop.getStore() == null ? null : shop.getStore().getName())
                .createdAt(DateFormats.dateTime(shop.getCreatedAt()))
                .build();
    }
}
