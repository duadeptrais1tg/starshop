package com.starshop.dto.order;

import com.starshop.entity.enums.AssignmentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một đơn trong danh sách của shipper (theo từng lần phân công).
 */
@Getter
@Builder
public class ShipperOrderRow {

    /** Id lần phân công (dùng trong URL /shipper/orders/{id}). */
    private final Long id;
    private final String orderCode;
    private final String assignedAt;
    private final String shopName;
    private final String receiverName;
    private final String receiverPhone;
    private final String shippingAddress;
    /** Tiền phải thu hộ (0 nếu đã thanh toán online). */
    private final BigDecimal codAmount;
    private final AssignmentStatus status;
}
