package com.starshop.dto.promotion;

import com.starshop.entity.enums.PromotionScope;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Ảnh chụp các khuyến mãi giảm % tự áp dụng đang chạy, dùng để tính giá khuyến mãi của nhiều sản phẩm
 * mà không phải query lại cho từng sản phẩm (tạo 1 lần cho mỗi trang danh sách).
 */
public class AutoPricing {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final List<Rule> rules;
    /** categoryId -> parentId, để khuyến mãi của danh mục cha áp dụng cả cho danh mục con. */
    private final Map<Long, Long> categoryParents;

    public AutoPricing(List<Rule> rules, Map<Long, Long> categoryParents) {
        this.rules = rules;
        this.categoryParents = categoryParents;
    }

    public static AutoPricing none() {
        return new AutoPricing(List.of(), Map.of());
    }

    /**
     * Giá sau khuyến mãi tốt nhất cho khách; null nếu không có khuyến mãi nào áp dụng.
     */
    public BigDecimal salePrice(Long shopId, Long categoryId, BigDecimal price) {
        BigDecimal bestDiscount = BigDecimal.ZERO;
        for (Rule rule : rules) {
            if (!rule.appliesTo(shopId, categoryId, categoryParents)) {
                continue;
            }
            BigDecimal discount = price.multiply(rule.percent()).divide(HUNDRED, 0, RoundingMode.HALF_UP);
            if (rule.maxDiscount() != null && discount.compareTo(rule.maxDiscount()) > 0) {
                discount = rule.maxDiscount();
            }
            if (discount.compareTo(bestDiscount) > 0) {
                bestDiscount = discount;
            }
        }
        return bestDiscount.signum() > 0 ? price.subtract(bestDiscount).max(BigDecimal.ZERO) : null;
    }

    /**
     * Một khuyến mãi giảm %: toàn sàn, theo danh mục (kể cả danh mục con) hoặc theo shop.
     */
    public record Rule(PromotionScope scope, Long shopId, Long categoryId, BigDecimal percent, BigDecimal maxDiscount) {

        boolean appliesTo(Long productShopId, Long productCategoryId, Map<Long, Long> parents) {
            return switch (scope) {
                case PLATFORM -> true;
                case SHOP -> Objects.equals(shopId, productShopId);
                case CATEGORY -> {
                    // Đi ngược từ danh mục sản phẩm lên các danh mục cha (giới hạn độ sâu tránh vòng lặp)
                    Long current = productCategoryId;
                    for (int depth = 0; current != null && depth < 20; depth++) {
                        if (current.equals(categoryId)) {
                            yield true;
                        }
                        current = parents.get(current);
                    }
                    yield false;
                }
            };
        }
    }
}
