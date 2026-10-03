package com.starshop.dto.promotion;

import com.starshop.entity.enums.PromotionScope;
import com.starshop.entity.enums.PromotionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Chương trình khuyến mãi hiển thị ở trang quản lý.
 */
@Getter
@Builder
public class PromotionDto {

    private final Long id;
    private final String name;
    private final PromotionType type;
    private final PromotionScope scope;
    private final String categoryName;
    private final BigDecimal discountValue;
    private final BigDecimal maxDiscount;
    private final BigDecimal minOrderValue;
    private final String startAt;
    private final String endAt;
    private final PromotionStatus status;
    private final boolean active;
    /** null = tự áp dụng. */
    private final String couponCode;
    private final Integer usageLimit;
    private final int usedCount;
    /** Đã có người dùng -> khóa sửa giá trị, không xóa được. */
    private final boolean locked;

    public boolean isPercent() {
        return type == PromotionType.PRODUCT_PERCENT;
    }
}
