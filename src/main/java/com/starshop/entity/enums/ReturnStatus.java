package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái yêu cầu trả hàng.
 */
@Getter
@RequiredArgsConstructor
public enum ReturnStatus {
    PENDING("Chờ duyệt"),
    APPROVED("Đã chấp nhận"),
    REJECTED("Bị từ chối");

    private final String label;
}
