package com.starshop.repository;

import com.starshop.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeIgnoreCase(String code);

    List<Coupon> findByPromotionIdInAndActiveTrue(Collection<Long> promotionIds);

    List<Coupon> findByPromotionId(Long promotionId);

    List<Coupon> findByPromotionIdIn(Collection<Long> promotionIds);

    /** Mã đang bật của chương trình đang chạy, áp dụng được cho shop (toàn sàn, theo danh mục, hoặc của chính shop). */
    @Query("select c from Coupon c join fetch c.promotion p left join p.shop s"
            + " where c.active = true and p.active = true and p.startAt <= :now and p.endAt >= :now"
            + " and (p.scope <> com.starshop.entity.enums.PromotionScope.SHOP or s.id = :shopId)")
    List<Coupon> findUsableForShop(@Param("now") LocalDateTime now, @Param("shopId") Long shopId);

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndPromotionIdNot(String code, Long promotionId);

    /**
     * Tăng lượt dùng nếu còn lượt (1 câu UPDATE có điều kiện -> an toàn khi nhiều người dùng mã cùng lúc).
     *
     * @return 1 nếu thành công, 0 nếu mã đã hết lượt
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Coupon c set c.usedCount = c.usedCount + 1"
            + " where c.id = :id and (c.usageLimit is null or c.usedCount < c.usageLimit)")
    int incrementUsage(@Param("id") Long id);

    /** Trả lại 1 lượt (hủy đơn / thanh toán thất bại). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Coupon c set c.usedCount = c.usedCount - 1 where c.id = :id and c.usedCount > 0")
    int decrementUsage(@Param("id") Long id);
}
