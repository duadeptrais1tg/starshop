package com.starshop.repository;

import com.starshop.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long>, JpaSpecificationExecutor<Promotion> {

    /**
     * Khuyến mãi đang chạy áp dụng được cho một sản phẩm: toàn sàn, theo danh mục của sản phẩm,
     * hoặc theo shop bán sản phẩm.
     */
    @Query("select p from Promotion p where p.active = true and p.startAt <= :now and p.endAt >= :now and ("
            + " p.scope = com.starshop.entity.enums.PromotionScope.PLATFORM"
            + " or (p.scope = com.starshop.entity.enums.PromotionScope.CATEGORY and p.category.id = :categoryId)"
            + " or (p.scope = com.starshop.entity.enums.PromotionScope.SHOP and p.shop.id = :shopId))"
            + " order by p.endAt asc")
    List<Promotion> findActiveForProduct(@Param("now") LocalDateTime now,
                                         @Param("shopId") Long shopId,
                                         @Param("categoryId") Long categoryId);

    /**
     * Khuyến mãi giảm % sản phẩm TỰ ÁP DỤNG (không có mã), đang chạy, không yêu cầu đơn tối thiểu:
     * dùng để hiện giá khuyến mãi ngay trên card / trang chi tiết.
     */
    @Query("select p from Promotion p where p.active = true and p.startAt <= :now and p.endAt >= :now"
            + " and p.type = com.starshop.entity.enums.PromotionType.PRODUCT_PERCENT and p.minOrderValue = 0"
            + " and not exists (select c from Coupon c where c.promotion = p)")
    List<Promotion> findRunningAutoProductPromotions(@Param("now") LocalDateTime now);
}
