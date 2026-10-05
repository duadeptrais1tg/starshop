package com.starshop.dto.checkout;

import com.starshop.entity.enums.PaymentMethod;
import com.starshop.entity.enums.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trang đặt hàng thành công: các đơn được tạo trong một lần thanh toán.
 */
@Getter
@Builder
public class OrderSuccessView {

    private final String txnRef;
    private final List<PlacedOrderDto> orders;
    private final PaymentMethod paymentMethod;
    /** Null với dữ liệu cũ không có Payment. */
    private final PaymentStatus paymentStatus;
    /** Lý do hủy (thanh toán thất bại). */
    private final String cancelReason;
    private final String paymentMethodLabel;
    private final BigDecimal total;
    private final String receiverName;
    private final String receiverPhone;
    private final String shippingAddress;

    public boolean isOnline() {
        return paymentMethod != PaymentMethod.COD;
    }

    public boolean isPaid() {
        return paymentStatus == PaymentStatus.PAID;
    }

    /** Thanh toán online thất bại / bị hủy / quá hạn: các đơn đã bị hủy. */
    public boolean isFailed() {
        return isOnline() && (paymentStatus == PaymentStatus.FAILED || paymentStatus == PaymentStatus.CANCELLED);
    }

    /** Thanh toán online chưa có kết quả (khách đóng trang VNPAY, IPN chưa về...). */
    public boolean isAwaitingPayment() {
        return isOnline() && paymentStatus == PaymentStatus.PENDING;
    }
}
