package com.starshop.repository;

import com.starshop.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    /** Số lần user đã dùng một mã (kiểm tra perUserLimit). */
    long countByCouponIdAndUserId(Long couponId, Long userId);

    /** Khuyến mãi đã có người dùng mã chưa (để khóa sửa giá trị / chặn xóa). */
    boolean existsByCouponPromotionId(Long promotionId);

    List<CouponUsage> findByOrderId(Long orderId);
}
