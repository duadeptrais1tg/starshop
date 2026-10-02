package com.starshop.dto.product;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Dữ liệu cho card sản phẩm dùng chung (trang chủ, tìm kiếm, danh mục, yêu thích...).
 */
@Getter
@Builder
public class ProductCardDto {

    private final Long id;
    private final String name;
    private final String slug;
    private final String imageUrl;
    private final String shopName;
    /** Giá bán hiện tại. */
    private final BigDecimal price;
    /** Giá gốc (gạch ngang) nếu cao hơn giá bán. */
    private final BigDecimal originalPrice;
    /** Giá sau khuyến mãi đang áp dụng (null = không có khuyến mãi; điền khi có chức năng khuyến mãi). */
    private final BigDecimal salePrice;
    private final int soldCount;
    private final BigDecimal ratingAvg;
    private final int reviewCount;
    private final boolean inStock;

    /** Giá khách thực trả. */
    public BigDecimal getFinalPrice() {
        return salePrice != null ? salePrice : price;
    }

    /** Giá hiển thị gạch ngang: giá gốc, hoặc giá bán nếu đang có khuyến mãi. */
    public BigDecimal getCompareAtPrice() {
        BigDecimal compare = salePrice != null ? price : originalPrice;
        if (compare == null || compare.compareTo(getFinalPrice()) <= 0) {
            return null;
        }
        return compare;
    }

    /** % giảm so với giá gạch ngang (0 = không giảm). */
    public int getDiscountPercent() {
        BigDecimal compare = getCompareAtPrice();
        if (compare == null) {
            return 0;
        }
        return compare.subtract(getFinalPrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(compare, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /** Số sao làm tròn 0.5 để vẽ: 4.3 -> 4.5 sao. */
    public double getRatingRounded() {
        return ratingAvg == null ? 0 : Math.round(ratingAvg.doubleValue() * 2) / 2.0;
    }
}
