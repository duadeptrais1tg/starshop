package com.starshop.dto.product;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một dòng trong danh sách sản phẩm của vendor.
 */
@Getter
@Builder
public class VendorProductRow {

    private final Long id;
    private final String name;
    private final String slug;
    private final String imageUrl;
    private final String categoryName;
    private final BigDecimal price;
    private final BigDecimal originalPrice;
    private final int stock;
    private final int soldCount;
    private final BigDecimal ratingAvg;
    private final int reviewCount;
    private final boolean active;
    /** Danh mục đang bị ẩn: khách không thấy sản phẩm dù đang bán. */
    private final boolean categoryHidden;

    public boolean isOutOfStock() {
        return stock <= 0;
    }
}
