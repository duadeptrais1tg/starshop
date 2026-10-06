package com.starshop.dto.order;

import com.starshop.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chi tiết đơn ở trang của khách, kèm quyền hủy / trả hàng theo trạng thái hiện tại.
 */
@Getter
@Builder
public class UserOrderDetail {

    private final Long id;
    private final String code;
    private final String createdAt;
    private final OrderStatus status;
    private final String shopName;
    private final String shopSlug;
    private final String receiverName;
    private final String receiverPhone;
    private final String shippingAddress;
    private final String note;
    private final String cancelReason;
    private final String carrierName;
    private final String shipperName;
    private final String shipperPhone;
    private final String deliveredAt;

    private final String paymentMethodLabel;
    private final String paymentStatusLabel;
    private final boolean paid;
    private final boolean awaitingPayment;
    private final String paymentTxnRef;
    private final String couponCode;

    private final BigDecimal subtotal;
    private final BigDecimal productDiscount;
    private final BigDecimal shippingFee;
    private final BigDecimal shippingDiscount;
    private final BigDecimal total;

    private final List<Line> items;
    private final List<History> history;
    private final ReturnInfo returnRequest;

    /** Hủy được: đơn còn chờ xác nhận và không đang chờ thanh toán online. */
    private final boolean canCancel;
    /** Yêu cầu trả hàng được: đã giao, còn trong hạn, chưa từng yêu cầu. */
    private final boolean canRequestReturn;
    /** Hạn cuối gửi yêu cầu trả hàng (khi đơn đã giao). */
    private final String returnDeadline;
    private final int returnDays;

    @Getter
    @Builder
    public static class Line {
        private final String productName;
        private final String productSlug;
        private final String imageUrl;
        private final BigDecimal unitPrice;
        private final int quantity;
        private final BigDecimal lineTotal;
    }

    @Getter
    @Builder
    public static class History {
        private final String time;
        private final String toLabel;
        private final String note;
    }

    @Getter
    @Builder
    public static class ReturnInfo {
        private final String reason;
        private final String statusLabel;
        private final String rejectReason;
        private final List<String> imageUrls;
        private final String createdAt;
    }
}
