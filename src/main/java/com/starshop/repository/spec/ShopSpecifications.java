package com.starshop.repository.spec;

import com.starshop.entity.Shop;
import com.starshop.entity.User;
import com.starshop.entity.enums.ShopStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Điều kiện tìm kiếm shop cho trang quản trị. Điều kiện null = không lọc.
 */
public final class ShopSpecifications {

    private ShopSpecifications() {
    }

    public static Specification<Shop> status(ShopStatus status) {
        return status == null ? null : (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    /** Tên shop, email hoặc tên chủ shop chứa từ khóa. */
    public static Specification<Shop> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(keyword);
        return (root, query, cb) -> {
            Join<Shop, User> owner = root.join("owner");
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, SpecUtils.ESCAPE),
                    cb.like(cb.lower(owner.get("email")), pattern, SpecUtils.ESCAPE),
                    cb.like(cb.lower(owner.get("fullName")), pattern, SpecUtils.ESCAPE));
        };
    }
}
