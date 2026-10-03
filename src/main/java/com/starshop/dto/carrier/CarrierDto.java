package com.starshop.dto.carrier;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Nhà vận chuyển hiển thị ở trang quản lý.
 */
@Getter
@Builder
public class CarrierDto {

    private final Long id;
    private final String name;
    private final BigDecimal shippingFee;
    private final boolean active;
    /** Số shipper đang thuộc nhà vận chuyển này. */
    private final long shipperCount;
}
