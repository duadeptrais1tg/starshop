package com.starshop.repository;

import com.starshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    long countByCategoryId(Long categoryId);

    /**
     * Đếm sản phẩm theo từng danh mục trong một lần query (thay vì đếm từng danh mục).
     * Mỗi phần tử: [categoryId (Long), số sản phẩm (Long)].
     */
    @Query("select p.category.id, count(p) from Product p where p.category.id in :ids group by p.category.id")
    List<Object[]> countByCategoryIds(@Param("ids") Collection<Long> ids);

    /**
     * Thống kê sản phẩm đang hiển thị của một shop (trang /shop/{slug}): số sản phẩm, tổng đã bán,
     * tổng lượt đánh giá và tổng điểm (ratingAvg x reviewCount) để tính trung bình có trọng số.
     */
    @Query("select count(p) as productCount, coalesce(sum(p.soldCount), 0) as soldCount,"
            + " coalesce(sum(p.reviewCount), 0) as reviewCount, coalesce(sum(p.ratingAvg * p.reviewCount), 0) as ratingSum"
            + " from Product p where p.shop.id = :shopId and p.active = true and p.category.active = true")
    ShopStats shopStats(@Param("shopId") Long shopId);

    interface ShopStats {
        long getProductCount();

        long getSoldCount();

        long getReviewCount();

        BigDecimal getRatingSum();
    }

    /** Danh sách có phân trang, nạp sẵn shop để hiển thị tên shop trên card (tránh N+1). */
    @Override
    @EntityGraph(attributePaths = "shop")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}
