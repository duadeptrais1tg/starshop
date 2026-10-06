package com.starshop.dto.order;

import com.starshop.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một dòng trong danh sách đơn của vendor.
 */
@Getter
@Builder
public class VendorOrderRow {

    private final Long id;
    private final String code;
    private final String createdAt;
    private final String receiverName;
    private final String receiverPhone;
    private final int itemCount;
    private final BigDecimal total;
    private final String paymentMethodLabel;
    /** Đã thanh toán online (không phải thu tiền khi giao). */
    private final boolean paid;
    private final OrderStatus status;
    private final String carrierName;
}
