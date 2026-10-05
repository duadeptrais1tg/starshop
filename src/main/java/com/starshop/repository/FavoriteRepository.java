package com.starshop.repository;

import com.starshop.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /** Dọn dữ liệu trỏ tới sản phẩm trước khi xóa hẳn sản phẩm. */
    @Modifying
    @Query("delete from Favorite f where f.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);
}
