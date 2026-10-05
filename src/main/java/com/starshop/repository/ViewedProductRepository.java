package com.starshop.repository;

import com.starshop.entity.ViewedProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ViewedProductRepository extends JpaRepository<ViewedProduct, Long> {

    Optional<ViewedProduct> findByUserIdAndProductId(Long userId, Long productId);

    /**
     * Sản phẩm user đã xem, mới xem gần nhất trước. Chỉ lấy sản phẩm khách còn được thấy
     * (đang bán, shop đã duyệt, danh mục đang hiển thị).
     */
    @Query(value = "select v from ViewedProduct v join fetch v.product p join fetch p.shop s join p.category c"
            + " where v.user.id = :userId and p.active = true and c.active = true"
            + " and s.status = com.starshop.entity.enums.ShopStatus.APPROVED order by v.viewedAt desc",
            countQuery = "select count(v) from ViewedProduct v join v.product p join p.shop s join p.category c"
                    + " where v.user.id = :userId and p.active = true and c.active = true"
                    + " and s.status = com.starshop.entity.enums.ShopStatus.APPROVED")
    Page<ViewedProduct> findVisibleByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("delete from ViewedProduct v where v.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);

    /** Dọn dữ liệu trỏ tới sản phẩm trước khi xóa hẳn sản phẩm. */
    @Modifying
    @Query("delete from ViewedProduct v where v.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);
}
