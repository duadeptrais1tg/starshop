package com.starshop.repository.spec;

import com.starshop.entity.Carrier;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Điều kiện tìm kiếm nhà vận chuyển. Điều kiện null = không lọc.
 */
public final class CarrierSpecifications {

    private CarrierSpecifications() {
    }

    public static Specification<Carrier> nameContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(keyword);
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, SpecUtils.ESCAPE);
    }

    public static Specification<Carrier> active(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
