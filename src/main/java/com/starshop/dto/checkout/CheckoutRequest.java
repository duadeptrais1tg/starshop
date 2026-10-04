package com.starshop.dto.checkout;

import com.starshop.entity.enums.PaymentMethod;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lựa chọn của khách ở trang checkout. Chỉ chứa id / mã, KHÔNG chứa giá: mọi số tiền tính lại ở server.
 */
@Getter
@Setter
public class CheckoutRequest {

    /** Id các dòng giỏ hàng được chọn mua. */
    private List<Long> itemIds = new ArrayList<>();
    private Long addressId;
    private Long carrierId;
    private PaymentMethod paymentMethod;
    /** shopId -> mã giảm giá khách nhập / chọn cho đơn của shop đó. */
    private Map<Long, String> coupons = new HashMap<>();
    /** shopId -> ghi chú cho shop. */
    private Map<Long, String> notes = new HashMap<>();
}
