package com.starshop.dto.promotion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái hiển thị của chương trình khuyến mãi (tính từ cờ active và thời gian).
 */
@Getter
@RequiredArgsConstructor
public enum PromotionStatus {
    RUNNING("Đang chạy"),
    UPCOMING("Sắp diễn ra"),
    ENDED("Đã kết thúc"),
    INACTIVE("Đã tắt");

    private final String label;
}
