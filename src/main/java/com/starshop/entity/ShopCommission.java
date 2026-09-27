package com.starshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Chiết khấu app thu của shop (%). shop = null là mức mặc định toàn sàn.
 */
@Entity
@Table(name = "shop_commissions")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ShopCommission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shop_id")
    private Shop shop;

    /** Tỉ lệ 0–100 (%). */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    /** Null = còn hiệu lực đến khi có mức mới. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;
}
