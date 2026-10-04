package com.starshop.dto.checkout;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Trang đặt hàng thành công: các đơn được tạo trong một lần thanh toán.
 */
@Getter
@Builder
public class OrderSuccessView {

    private final List<PlacedOrderDto> orders;
    private final String paymentMethodLabel;
    private final BigDecimal total;
    private final String receiverName;
    private final String receiverPhone;
    private final String shippingAddress;
}
