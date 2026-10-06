package com.starshop.dto.order;

import com.starshop.entity.enums.AssignmentStatus;
import com.starshop.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chi tiết đơn ở trang shipper: lấy hàng ở đâu, giao cho ai, thu bao nhiêu, và các thao tác được phép.
 */
@Getter
@Builder
public class ShipperOrderDetail {

    private final Long id;
    private final String orderCode;
    private final OrderStatus orderStatus;
    private final AssignmentStatus status;
    private final String assignedAt;
    private final String deliveredAt;
    private final String failReason;

    private final String shopName;
    private final String shopPhone;
    private final String pickupAddress;

    private final String receiverName;
    private final String receiverPhone;
    private final String shippingAddress;
    private final String note;

    private final String paymentMethodLabel;
    /** Đơn COD: shipper thu tiền khi giao. */
    private final boolean cod;
    private final BigDecimal codAmount;
    private final boolean codCollected;
    private final String collectedAt;

    private final List<Line> items;

    /** Lần phân công này còn hiệu lực (đơn chưa được giao cho shipper khác). */
    private final boolean current;

    public boolean isCanStart() {
        return current && status == AssignmentStatus.ASSIGNED;
    }

    public boolean isCanFinish() {
        return current && status == AssignmentStatus.DELIVERING;
    }

    public boolean isCanRetry() {
        return current && status == AssignmentStatus.FAILED && orderStatus == OrderStatus.SHIPPING;
    }

    @Getter
    @Builder
    public static class Line {
        private final String productName;
        private final int quantity;
    }
}
