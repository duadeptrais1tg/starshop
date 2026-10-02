package com.starshop.repository.spec;

import com.starshop.entity.Category;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

/**
 * Điều kiện tìm kiếm danh mục. Điều kiện null = không lọc.
 */
public final class CategorySpecifications {

    private CategorySpecifications() {
    }

    /** Tên hoặc slug chứa từ khóa. */
    public static Specification<Category> keyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(keyword);
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern, SpecUtils.ESCAPE),
                cb.like(root.get("slug"), pattern, SpecUtils.ESCAPE));
    }

    public static Specification<Category> active(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
