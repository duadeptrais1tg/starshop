package com.starshop.dto.product;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dữ liệu trang chi tiết sản phẩm.
 */
@Getter
@Builder
public class ProductDetailDto {

    private final Long id;
    private final String name;
    private final String slug;
    private final String description;
    private final List<String> imageUrls;
    /** Giá, giá gạch ngang, % giảm, sao: dùng lại logic của card. */
    private final ProductCardDto pricing;
    private final int stock;
    private final int favoriteCount;
    private final Long shopId;
    private final String shopName;
    private final String shopSlug;
    private final Long categoryId;
    private final String categoryName;
    private final String categorySlug;
    private final List<PromotionInfo> promotions;

    public BigDecimal getFinalPrice() {
        return pricing.getFinalPrice();
    }

    public boolean isInStock() {
        return stock > 0;
    }
}
