package com.starshop.mapper;

import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductDetailDto;
import com.starshop.dto.product.PromotionInfo;
import com.starshop.entity.Product;
import com.starshop.entity.Promotion;
import com.starshop.util.DateFormats;

import java.math.BigDecimal;
import java.util.List;

public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * @param imageUrl ảnh đại diện (lấy riêng bằng 1 query cho cả trang)
     */
    public static ProductCardDto toCard(Product product, String imageUrl) {
        return toCard(product, imageUrl, null);
    }

    /**
     * @param salePrice giá sau khuyến mãi tự áp dụng (null = không có khuyến mãi)
     */
    public static ProductCardDto toCard(Product product, String imageUrl, BigDecimal salePrice) {
        return ProductCardDto.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .imageUrl(imageUrl)
                .shopName(product.getShop().getName())
                .price(product.getPrice())
                .originalPrice(product.getOriginalPrice())
                .salePrice(salePrice)
                .soldCount(product.getSoldCount())
                .ratingAvg(product.getRatingAvg())
                .reviewCount(product.getReviewCount())
                .inStock(product.getStock() > 0)
                .build();
    }

    public static ProductDetailDto toDetail(Product product, List<String> imageUrls, List<PromotionInfo> promotions,
                                            BigDecimal salePrice) {
        return ProductDetailDto.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .imageUrls(imageUrls)
                .pricing(toCard(product, imageUrls.isEmpty() ? null : imageUrls.get(0), salePrice))
                .stock(product.getStock())
                .favoriteCount(product.getFavoriteCount())
                .shopId(product.getShop().getId())
                .shopName(product.getShop().getName())
                .shopSlug(product.getShop().getSlug())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .categorySlug(product.getCategory().getSlug())
                .promotions(promotions)
                .build();
    }

    public static PromotionInfo toPromotionInfo(Promotion promotion, List<String> couponCodes) {
        return PromotionInfo.builder()
                .name(promotion.getName())
                .description(promotion.getDescription())
                .type(promotion.getType())
                .discountValue(promotion.getDiscountValue())
                .maxDiscount(promotion.getMaxDiscount())
                .minOrderValue(promotion.getMinOrderValue())
                .endAt(DateFormats.dateTime(promotion.getEndAt()))
                .couponCodes(couponCodes)
                .build();
    }
}
