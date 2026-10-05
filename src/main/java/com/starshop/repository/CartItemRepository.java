package com.starshop.repository;

import com.starshop.entity.CartItem;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    /** Giỏ của user, nạp sẵn sản phẩm + shop + danh mục (tránh N+1); mới thêm lên trước. */
    @EntityGraph(attributePaths = {"product", "product.shop", "product.category"})
    List<CartItem> findByCartUserIdOrderByIdDesc(Long userId);

    /** Chỉ tìm trong giỏ của chính user -> không sửa / xóa được dòng của người khác. */
    @EntityGraph(attributePaths = {"product", "product.shop", "product.category"})
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"product", "product.shop", "product.category"})
    List<CartItem> findByIdInAndCartUserId(Collection<Long> ids, Long userId);

    /** Xóa các dòng đã đặt hàng (1 câu DELETE, chỉ trong giỏ của user). */
    @Modifying
    @Query("delete from CartItem ci where ci.id in :ids and ci.cart.id = :cartId")
    int deleteOrdered(@Param("ids") Collection<Long> ids, @Param("cartId") Long cartId);

    /** Số dòng trong giỏ (hiện trên badge header). */
    long countByCartUserId(Long userId);

    /** Dọn dữ liệu trỏ tới sản phẩm trước khi xóa hẳn sản phẩm. */
    @Modifying
    @Query("delete from CartItem ci where ci.product.id = :productId")
    int deleteByProductId(@Param("productId") Long productId);
}
