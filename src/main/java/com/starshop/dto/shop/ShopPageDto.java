package com.starshop.dto.shop;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Thông tin công khai của shop cho trang /shop/{slug}.
 */
@Getter
@Builder
public class ShopPageDto {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final String logoUrl;
    private final String bannerUrl;
    private final String joinedAt;
    private final long productCount;
    private final long soldCount;
    private final long reviewCount;
    /** Đánh giá trung bình của các sản phẩm (theo số lượt đánh giá), làm tròn 1 chữ số; 0 nếu chưa có. */
    private final BigDecimal ratingAvg;

    /** Làm tròn 0.5 để vẽ sao. */
    public double getRatingRounded() {
        return ratingAvg == null ? 0 : Math.round(ratingAvg.doubleValue() * 2) / 2.0;
    }

    public String getInitial() {
        return name == null || name.isBlank() ? "S" : name.substring(0, 1).toUpperCase();
    }
}
