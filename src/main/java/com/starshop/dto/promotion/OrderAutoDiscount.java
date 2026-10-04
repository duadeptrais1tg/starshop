package com.starshop.dto.promotion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Khuyến mãi tự áp dụng (không cần mã) ở cấp đơn hàng: giảm % có đơn tối thiểu và giảm phí vận chuyển.
 * Mỗi loại chọn chương trình có lợi nhất cho khách.
 *
 * @param productDiscount  số tiền giảm trên tiền hàng
 * @param shippingDiscount số tiền giảm trên phí vận chuyển
 * @param promotionNames   tên các chương trình được áp dụng
 */
public record OrderAutoDiscount(BigDecimal productDiscount, BigDecimal shippingDiscount, List<String> promotionNames) {
}
