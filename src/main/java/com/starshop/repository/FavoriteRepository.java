package com.starshop.repository;

import com.starshop.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);

    /** Dọn dữ liệu trỏ tới sản phẩm trước khi xóa hẳn sản phẩm. */
    @Modifying
    @Query("delete from Favorite f where f.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);

    /** Sản phẩm (trong danh sách cho trước) mà user đã thích -> tô đỏ nút tim trên card. */
    @Query("select f.product.id from Favorite f where f.user.id = :userId and f.product.id in :productIds")
    List<Long> findLikedProductIds(@Param("userId") Long userId, @Param("productIds") Collection<Long> productIds);

    /** Yêu thích của user, chỉ sản phẩm khách còn xem được; mới thích lên trước. */
    @Query(value = "select f from Favorite f join fetch f.product p join fetch p.shop s join p.category c"
            + " where f.user.id = :userId and p.active = true and c.active = true"
            + " and s.status = com.starshop.entity.enums.ShopStatus.APPROVED order by f.id desc",
            countQuery = "select count(f) from Favorite f join f.product p join p.shop s join p.category c"
                    + " where f.user.id = :userId and p.active = true and c.active = true"
                    + " and s.status = com.starshop.entity.enums.ShopStatus.APPROVED")
    Page<Favorite> findVisibleByUserId(@Param("userId") Long userId, Pageable pageable);
}
