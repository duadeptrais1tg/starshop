package com.starshop.dto.checkout;

import com.starshop.dto.promotion.CouponOption;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Đơn hàng của một shop ở trang checkout (mỗi shop tách thành một đơn).
 */
@Getter
@Builder
public class CheckoutGroup {

    private final Long shopId;
    private final String shopName;
    private final String shopSlug;
    private final List<CheckoutLine> lines;
    private final BigDecimal subtotal;
    private final BigDecimal shippingFee;

    /** Khuyến mãi tự áp dụng cấp đơn (đơn tối thiểu / giảm phí ship). */
    private final BigDecimal autoProductDiscount;
    private final BigDecimal autoShippingDiscount;
    private final List<String> autoPromotionNames;

    /** Mã khách nhập (đã chuẩn hóa) và kết quả áp mã. */
    private final String couponCode;
    private final String couponName;
    private final BigDecimal couponProductDiscount;
    private final BigDecimal couponShippingDiscount;
    /** Mã không dùng được: lý do (đơn không được đặt cho đến khi bỏ / đổi mã). */
    private final String couponError;
    private final List<CouponOption> couponOptions;

    private final String note;

    /** Tổng giảm trên tiền hàng, không vượt quá tiền hàng. */
    public BigDecimal getProductDiscount() {
        return autoProductDiscount.add(couponProductDiscount).min(subtotal);
    }

    /** Tổng giảm phí ship, không vượt quá phí ship. */
    public BigDecimal getShippingDiscount() {
        return autoShippingDiscount.add(couponShippingDiscount).min(shippingFee);
    }

    public BigDecimal getTotal() {
        return subtotal.subtract(getProductDiscount()).add(shippingFee).subtract(getShippingDiscount());
    }

    public boolean isCouponApplied() {
        return couponCode != null && couponError == null;
    }

    public boolean isHasUnavailable() {
        return lines.stream().anyMatch(l -> l.getUnavailableReason() != null);
    }
}
