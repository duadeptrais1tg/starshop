package com.starshop.repository;

import com.starshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByOrderItemId(Long orderItemId);

    /** Đánh giá của sản phẩm, mới nhất trước; nạp sẵn người viết (media lấy riêng theo lô). */
    @EntityGraph(attributePaths = "user")
    Page<Review> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    /**
     * Đánh giá của sản phẩm có lọc: theo số sao (null = mọi mức) và/hoặc chỉ đánh giá có ảnh/video.
     */
    @EntityGraph(attributePaths = "user")
    @Query(value = "select r from Review r where r.product.id = :productId"
            + " and (:star is null or r.rating = :star)"
            + " and (:withMedia = false or exists (select m.id from ReviewMedia m where m.review = r))"
            + " order by r.createdAt desc, r.id desc",
            countQuery = "select count(r) from Review r where r.product.id = :productId"
                    + " and (:star is null or r.rating = :star)"
                    + " and (:withMedia = false or exists (select m.id from ReviewMedia m where m.review = r))")
    Page<Review> search(@Param("productId") Long productId, @Param("star") Integer star,
                        @Param("withMedia") boolean withMedia, Pageable pageable);

    /** Số đánh giá theo từng mức sao. Mỗi phần tử: [rating (Integer), số lượng (Long)]. */
    @Query("select r.rating, count(r) from Review r where r.product.id = :productId group by r.rating")
    List<Object[]> countByRating(@Param("productId") Long productId);

    /** Số đánh giá có ảnh hoặc video. */
    @Query("select count(r) from Review r where r.product.id = :productId"
            + " and exists (select m.id from ReviewMedia m where m.review = r)")
    long countWithMedia(@Param("productId") Long productId);

    /**
     * Tính lại điểm trung bình và số lượt đánh giá của sản phẩm từ bảng reviews (1 câu UPDATE,
     * luôn khớp dữ liệu kể cả khi nhiều người đánh giá cùng lúc).
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "update products p set"
            + " p.review_count = (select count(*) from reviews r where r.product_id = p.id),"
            + " p.rating_avg = (select coalesce(round(avg(r.rating), 2), 0) from reviews r where r.product_id = p.id)"
            + " where p.id = :productId", nativeQuery = true)
    int refreshProductRating(@Param("productId") Long productId);
}
