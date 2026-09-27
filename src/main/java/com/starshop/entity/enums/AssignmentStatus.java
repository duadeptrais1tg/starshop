package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái một lần phân công giao hàng cho shipper.
 */
@Getter
@RequiredArgsConstructor
public enum AssignmentStatus {
    ASSIGNED("Đã phân công"),
    DELIVERING("Đang giao"),
    DELIVERED("Giao thành công"),
    FAILED("Giao thất bại");

    private final String label;
}
