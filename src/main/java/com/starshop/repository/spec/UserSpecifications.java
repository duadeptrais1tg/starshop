package com.starshop.repository.spec;

import com.starshop.dto.admin.UserStatusFilter;
import com.starshop.entity.Role;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Điều kiện tìm kiếm user cho trang quản trị. Điều kiện null = không lọc.
 */
public final class UserSpecifications {

    private UserSpecifications() {
    }

    /** Tên, email hoặc SĐT chứa từ khóa (không phân biệt hoa thường). */
    public static Specification<User> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("fullName")), pattern, SpecUtils.ESCAPE),
                cb.like(cb.lower(root.get("email")), pattern, SpecUtils.ESCAPE),
                cb.like(root.get("phone"), pattern, SpecUtils.ESCAPE));
    }

    public static Specification<User> hasRole(RoleName role) {
        if (role == null) {
            return null;
        }
        return (root, query, cb) -> {
            query.distinct(true);
            Join<User, Role> roles = root.join("roles");
            return cb.equal(roles.get("name"), role);
        };
    }

    public static Specification<User> status(UserStatusFilter status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case ACTIVE -> (root, query, cb) -> cb.and(cb.isTrue(root.get("enabled")), cb.isFalse(root.get("locked")));
            case LOCKED -> (root, query, cb) -> cb.isTrue(root.get("locked"));
            case UNVERIFIED -> (root, query, cb) -> cb.isFalse(root.get("enabled"));
        };
    }
}
