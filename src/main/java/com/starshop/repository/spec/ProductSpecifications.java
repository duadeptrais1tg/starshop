package com.starshop.repository.spec;

import com.starshop.entity.Category;
import com.starshop.entity.Product;
import com.starshop.entity.Shop;
import com.starshop.entity.enums.ShopStatus;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Điều kiện lọc sản phẩm. Điều kiện null = không lọc.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /**
     * Sản phẩm khách được thấy: đang bán, thuộc shop đã duyệt, thuộc danh mục đang hiển thị.
     */
    public static Specification<Product> visibleToCustomers() {
        return (root, query, cb) -> {
            Join<Product, Shop> shop = root.join("shop");
            Join<Product, Category> category = root.join("category");
            return cb.and(
                    cb.isTrue(root.get("active")),
                    cb.equal(shop.get("status"), ShopStatus.APPROVED),
                    cb.isTrue(category.get("active")));
        };
    }

    /**
     * Tên chứa từ khóa. Collation utf8mb4_0900_ai_ci của MySQL không phân biệt dấu và hoa thường,
     * nên "hoa hong" vẫn tìm được "Hoa hồng".
     */
    public static Specification<Product> nameContains(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return null;
        }
        String pattern = SpecUtils.likePattern(keyword);
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, SpecUtils.ESCAPE);
    }

    public static Specification<Product> categoryIn(Collection<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return null;
        }
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    public static Specification<Product> priceFrom(BigDecimal min) {
        return min == null ? null : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("price"), min);
    }

    public static Specification<Product> priceTo(BigDecimal max) {
        return max == null ? null : (root, query, cb) -> cb.lessThanOrEqualTo(root.get("price"), max);
    }

    public static Specification<Product> shop(Long shopId) {
        return shopId == null ? null : (root, query, cb) -> cb.equal(root.get("shop").get("id"), shopId);
    }

    /** Điểm đánh giá trung bình từ minRating sao trở lên. */
    public static Specification<Product> ratingAtLeast(Integer minRating) {
        return minRating == null ? null
                : (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("ratingAvg"), BigDecimal.valueOf(minRating));
    }

    public static Specification<Product> soldMoreThan(int quantity) {
        return (root, query, cb) -> cb.greaterThan(root.get("soldCount"), quantity);
    }

    /** Đã có ít nhất một đánh giá. */
    public static Specification<Product> hasReviews() {
        return (root, query, cb) -> cb.greaterThan(root.get("reviewCount"), 0);
    }

    /** Đã có người thích. */
    public static Specification<Product> hasFavorites() {
        return (root, query, cb) -> cb.greaterThan(root.get("favoriteCount"), 0);
    }

    public static Specification<Product> idNot(Long id) {
        return id == null ? null : (root, query, cb) -> cb.notEqual(root.get("id"), id);
    }

    /** Lọc theo trạng thái bán: null = tất cả. */
    public static Specification<Product> active(Boolean active) {
        return active == null ? null : (root, query, cb) -> cb.equal(root.get("active"), active);
    }

    public static Specification<Product> outOfStock() {
        return (root, query, cb) -> cb.le(root.get("stock"), 0);
    }

    public static Specification<Product> category(Long categoryId) {
        return categoryId == null ? null : (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
    }
}
