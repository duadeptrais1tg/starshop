package com.starshop.dto.review;

import lombok.Builder;
import lombok.Getter;

/**
 * Sản phẩm (trong một đơn đã giao) đang được đánh giá: hiển thị ở đầu form.
 */
@Getter
@Builder
public class ReviewTarget {

    private final Long orderItemId;
    private final Long orderId;
    private final String orderCode;
    private final String productName;
    private final String productSlug;
    private final String imageUrl;
    private final int quantity;
}
