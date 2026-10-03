package com.starshop.repository;

import com.starshop.entity.ShopCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Lịch sử mức chiết khấu. shop = null là mức mặc định toàn sàn.
 */
@Repository
public interface ShopCommissionRepository extends JpaRepository<ShopCommission, Long> {

    /** Các mức của một shop, theo ngày bắt đầu. */
    List<ShopCommission> findByShopIdOrderByEffectiveFromAsc(Long shopId);

    /** Các mức mặc định toàn sàn, theo ngày bắt đầu. */
    List<ShopCommission> findByShopIsNullOrderByEffectiveFromAsc();

    /** Mức riêng đang hiệu lực vào ngày date của nhiều shop (cho danh sách shop). */
    @Query("select c from ShopCommission c where c.shop.id in :shopIds and c.effectiveFrom <= :date"
            + " and (c.effectiveTo is null or c.effectiveTo >= :date)")
    List<ShopCommission> findEffectiveForShops(@Param("shopIds") Collection<Long> shopIds, @Param("date") LocalDate date);

    /** Mức riêng của shop hiệu lực vào ngày date (tối đa 1 vì các giai đoạn không chồng nhau). */
    @Query("select c from ShopCommission c where c.shop.id = :shopId and c.effectiveFrom <= :date"
            + " and (c.effectiveTo is null or c.effectiveTo >= :date) order by c.effectiveFrom desc")
    List<ShopCommission> findEffectiveForShop(@Param("shopId") Long shopId, @Param("date") LocalDate date);

    /** Mức mặc định toàn sàn hiệu lực vào ngày date. */
    @Query("select c from ShopCommission c where c.shop is null and c.effectiveFrom <= :date"
            + " and (c.effectiveTo is null or c.effectiveTo >= :date) order by c.effectiveFrom desc")
    List<ShopCommission> findEffectiveDefault(@Param("date") LocalDate date);
}
