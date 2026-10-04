package com.starshop.dto.promotion;

import com.starshop.entity.enums.PromotionScope;
import com.starshop.entity.enums.PromotionType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một mã giảm giá khách dùng được cho đơn của một shop (hiển thị ở trang checkout).
 * Là class có getter (không dùng record) để JSP/EL đọc được.
 */
@Getter
@AllArgsConstructor
public class CouponOption {

    private final String code;
    private final String promotionName;
    private final PromotionType type;
    private final PromotionScope scope;
    /** Số tiền giảm nếu áp mã cho đơn hiện tại. */
    private final BigDecimal discount;
    /** Ví dụ "Giảm 10%, tối đa 50.000₫ – đơn từ 200.000₫". */
    private final String description;
    private final String endAt;
}
