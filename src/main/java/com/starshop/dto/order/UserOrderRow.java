package com.starshop.dto.order;

import com.starshop.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một đơn trong danh sách "Đơn hàng của tôi": hiện sản phẩm đầu tiên và số sản phẩm còn lại.
 */
@Getter
@Builder
public class UserOrderRow {

    private final Long id;
    private final String code;
    private final String createdAt;
    private final String shopName;
    private final String shopSlug;
    private final String firstProductName;
    private final String firstProductImage;
    private final int firstProductQuantity;
    /** Số dòng sản phẩm còn lại ngoài sản phẩm đầu tiên. */
    private final int moreProducts;
    private final BigDecimal total;
    private final OrderStatus status;
    /** Thanh toán online chưa xong (VNPAY đang chờ). */
    private final boolean awaitingPayment;
    private final String paymentTxnRef;
}
