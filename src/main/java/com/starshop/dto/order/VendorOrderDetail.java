package com.starshop.dto.order;

import com.starshop.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Chi tiết đơn ở trang vendor, kèm các hành động được phép theo trạng thái hiện tại.
 */
@Getter
@Builder
public class VendorOrderDetail {

    private final Long id;
    private final String code;
    private final String createdAt;
    private final OrderStatus status;
    private final String customerName;
    private final String receiverName;
    private final String receiverPhone;
    private final String shippingAddress;
    private final String note;
    private final String cancelReason;
    private final String carrierName;
    private final String paymentMethodLabel;
    private final String paymentStatusLabel;
    private final boolean paid;
    private final String couponCode;

    private final BigDecimal subtotal;
    private final BigDecimal productDiscount;
    private final BigDecimal shippingFee;
    private final BigDecimal shippingDiscount;
    private final BigDecimal total;
    /** Tiền shop nhận sau chiết khấu = (tiền hàng - giảm giá sản phẩm) x (1 - %chiết khấu). */
    private final BigDecimal commissionRate;
    private final BigDecimal shopReceives;

    private final List<Line> items;
    private final List<History> history;
    /** Null nếu chưa giao cho shipper. */
    private final Assignment assignment;
    /** Null nếu không có yêu cầu trả hàng. */
    private final ReturnInfo returnRequest;
    /** Shipper chọn được (khi đơn đã xác nhận hoặc cần giao lại). */
    private final List<ShipperOption> shippers;
    /** Lần giao gần nhất thất bại: được giao lại cho shipper khác (đơn vẫn Đang giao). */
    private final boolean reassignable;

    public boolean isCanConfirm() {
        return status == OrderStatus.NEW;
    }

    public boolean isCanCancel() {
        return status == OrderStatus.NEW || status == OrderStatus.CONFIRMED;
    }

    public boolean isCanAssign() {
        return status == OrderStatus.CONFIRMED || reassignable;
    }

    public boolean isCanReviewReturn() {
        return status == OrderStatus.RETURN_REQUESTED && returnRequest != null && returnRequest.isPending();
    }

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
        private final String fromLabel;
        private final String toLabel;
        private final String changedBy;
        private final String note;
    }

    @Getter
    @Builder
    public static class Assignment {
        private final String shipperName;
        private final String shipperPhone;
        private final String statusLabel;
        private final String failReason;
        private final String assignedAt;
    }

    @Getter
    @Builder
    public static class ReturnInfo {
        private final String reason;
        private final String statusLabel;
        private final boolean pending;
        private final String rejectReason;
        private final List<String> imageUrls;
        private final String createdAt;
    }

    @Getter
    @Builder
    public static class ShipperOption {
        private final Long id;
        private final String name;
        private final String phone;
        /** Số đơn shipper đang giao dở. */
        private final long activeOrders;
    }
}
