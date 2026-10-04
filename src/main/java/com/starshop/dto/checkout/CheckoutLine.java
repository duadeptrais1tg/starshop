package com.starshop.dto.checkout;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một sản phẩm trong đơn ở trang checkout.
 */
@Getter
@Builder
public class CheckoutLine {

    private final Long productId;
    private final String productName;
    private final String productSlug;
    private final String imageUrl;
    /** Đơn giá sau khuyến mãi tự áp dụng. */
    private final BigDecimal unitPrice;
    /** Giá gốc để gạch ngang (null nếu không giảm). */
    private final BigDecimal compareAtPrice;
    private final int quantity;
    private final BigDecimal lineTotal;
    /** Lý do không mua được (null = mua được). */
    private final String unavailableReason;
}
