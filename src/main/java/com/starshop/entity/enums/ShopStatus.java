package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái shop do Admin duyệt.
 */
@Getter
@RequiredArgsConstructor
public enum ShopStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đang hoạt động"),
    REJECTED("Bị từ chối"),
    SUSPENDED("Bị đình chỉ");

    private final String label;
}
