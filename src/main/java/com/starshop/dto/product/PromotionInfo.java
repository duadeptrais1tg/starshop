package com.starshop.dto.product;

import com.starshop.entity.enums.PromotionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Khuyến mãi đang áp dụng, hiển thị ở trang chi tiết sản phẩm.
 */
@Getter
@Builder
public class PromotionInfo {

    private final String name;
    private final String description;
    private final PromotionType type;
    /** % (PRODUCT_PERCENT) hoặc số tiền giảm phí ship (SHIPPING_DISCOUNT). */
    private final BigDecimal discountValue;
    private final BigDecimal maxDiscount;
    private final BigDecimal minOrderValue;
    private final String endAt;
    /** Mã cần nhập (rỗng = tự áp dụng). */
    private final List<String> couponCodes;

    public boolean isPercent() {
        return type == PromotionType.PRODUCT_PERCENT;
    }
}
