package com.starshop.dto.cart;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một dòng trong giỏ. Giá luôn là giá hiện tại tính ở server (không lưu giá vào giỏ).
 */
@Getter
@Builder
public class CartLineDto {

    private final Long itemId;
    private final Long productId;
    private final String productName;
    private final String productSlug;
    private final String imageUrl;
    /** Đơn giá khách trả (đã gồm khuyến mãi tự áp dụng). */
    private final BigDecimal unitPrice;
    /** Giá gạch ngang (giá gốc / giá trước khuyến mãi), null nếu không có. */
    private final BigDecimal compareAtPrice;
    private final int quantity;
    private final int stock;
    /** null = mua được; ngược lại là lý do không cho chọn thanh toán. */
    private final String unavailableReason;

    public boolean isAvailable() {
        return unavailableReason == null;
    }

    public BigDecimal getLineTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
