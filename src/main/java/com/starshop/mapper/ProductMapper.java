package com.starshop.mapper;

import com.starshop.dto.product.ProductCardDto;
import com.starshop.entity.Product;

public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * @param imageUrl ảnh đại diện (lấy riêng bằng 1 query cho cả trang)
     */
    public static ProductCardDto toCard(Product product, String imageUrl) {
        return ProductCardDto.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .imageUrl(imageUrl)
                .shopName(product.getShop().getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .soldCount(product.getSoldCount())
                .ratingAvg(product.getRatingAvg())
                .reviewCount(product.getReviewCount())
                .inStock(product.getStock() > 0)
                .build();
    }
}
