package com.starshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Mã giảm giá gắn với một Promotion.
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Coupon extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /** Tổng số lượt dùng tối đa (null = không giới hạn). */
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Builder.Default
    @Column(name = "used_count", nullable = false)
    private int usedCount = 0;

    /** Số lần tối đa mỗi user được dùng. */
    @Builder.Default
    @Column(name = "per_user_limit", nullable = false)
    private int perUserLimit = 1;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
