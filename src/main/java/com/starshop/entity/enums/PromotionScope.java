package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Phạm vi áp dụng khuyến mãi.
 */
@Getter
@RequiredArgsConstructor
public enum PromotionScope {
    PLATFORM("Toàn sàn"),
    CATEGORY("Theo danh mục"),
    SHOP("Theo shop");

    private final String label;
}
