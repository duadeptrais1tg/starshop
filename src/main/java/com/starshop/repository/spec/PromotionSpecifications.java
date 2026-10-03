package com.starshop.repository.spec;

import com.starshop.dto.promotion.PromotionStatus;
import com.starshop.entity.Promotion;
import com.starshop.entity.enums.PromotionScope;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * Điều kiện lọc chương trình khuyến mãi. Điều kiện null = không lọc.
 */
public final class PromotionSpecifications {

    private PromotionSpecifications() {
    }

    public static Specification<Promotion> nameContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(keyword);
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, SpecUtils.ESCAPE);
    }

    /** Khuyến mãi do Admin quản lý: toàn sàn hoặc theo danh mục (khuyến mãi của shop do Vendor quản lý). */
    public static Specification<Promotion> platformManaged() {
        return (root, query, cb) -> root.get("scope").in(PromotionScope.PLATFORM, PromotionScope.CATEGORY);
    }

    public static Specification<Promotion> status(PromotionStatus status, LocalDateTime now) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case INACTIVE -> (root, query, cb) -> cb.isFalse(root.get("active"));
            case UPCOMING -> (root, query, cb) -> cb.and(cb.isTrue(root.get("active")), cb.greaterThan(root.get("startAt"), now));
            case ENDED -> (root, query, cb) -> cb.and(cb.isTrue(root.get("active")), cb.lessThan(root.get("endAt"), now));
            case RUNNING -> (root, query, cb) -> cb.and(cb.isTrue(root.get("active")),
                    cb.lessThanOrEqualTo(root.get("startAt"), now), cb.greaterThanOrEqualTo(root.get("endAt"), now));
        };
    }
}
