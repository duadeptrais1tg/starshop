package com.starshop.dto.checkout;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Một đơn vừa đặt, hiển thị ở trang đặt hàng thành công.
 */
@Getter
@Builder
public class PlacedOrderDto {

    private final String code;
    private final String shopName;
    private final int itemCount;
    private final BigDecimal total;
    private final String statusLabel;
}
