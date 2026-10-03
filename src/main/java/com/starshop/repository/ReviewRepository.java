package com.starshop.repository;

import com.starshop.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /** Số đánh giá theo từng mức sao. Mỗi phần tử: [rating (Integer), số lượng (Long)]. */
    @Query("select r.rating, count(r) from Review r where r.product.id = :productId group by r.rating")
    List<Object[]> countByRating(@Param("productId") Long productId);
}
