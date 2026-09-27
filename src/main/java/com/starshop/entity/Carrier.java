package com.starshop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Nhà vận chuyển (dùng chung toàn chuỗi). Shipper thuộc về một Carrier.
 */
@Entity
@Table(name = "carriers")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Carrier extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}
