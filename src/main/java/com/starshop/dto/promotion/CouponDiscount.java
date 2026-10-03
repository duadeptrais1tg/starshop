package com.starshop.dto.promotion;

import com.starshop.entity.enums.PromotionType;

import java.math.BigDecimal;

/**
 * Kết quả áp mã giảm giá cho một đơn (tính ở server, không tin số tiền gửi từ trình duyệt).
 *
 * @param couponId         id coupon, truyền lại cho recordUsage khi tạo đơn
 * @param code             mã đã chuẩn hóa (chữ in hoa)
 * @param promotionName    tên chương trình
 * @param type             giảm % sản phẩm hay giảm phí vận chuyển
 * @param productDiscount  số tiền giảm trên tiền hàng
 * @param shippingDiscount số tiền giảm trên phí vận chuyển
 */
public record CouponDiscount(Long couponId, String code, String promotionName, PromotionType type,
                             BigDecimal productDiscount, BigDecimal shippingDiscount) {

    public BigDecimal total() {
        return productDiscount.add(shippingDiscount);
    }
}
