package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Phương thức thanh toán.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    COD("Thanh toán khi nhận hàng"),
    VNPAY("VNPAY"),
    MOMO("Ví MoMo");

    private final String label;
}
