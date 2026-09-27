package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Loại khuyến mãi.
 */
@Getter
@RequiredArgsConstructor
public enum PromotionType {
    PRODUCT_PERCENT("Giảm % giá sản phẩm"),
    SHIPPING_DISCOUNT("Giảm phí vận chuyển");

    private final String label;
}
