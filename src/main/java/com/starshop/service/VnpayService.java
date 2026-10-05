package com.starshop.service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Làm việc với cổng VNPAY (phiên bản API 2.1.0): tạo URL thanh toán có chữ ký và kiểm tra chữ ký callback.
 * Chỉ lo phần giao thức; nghiệp vụ cập nhật Payment / đơn hàng nằm ở PaymentService.
 */
public interface VnpayService {

    boolean isEnabled();

    /**
     * URL chuyển khách sang trang thanh toán VNPAY.
     *
     * @param txnRef   mã giao dịch của StarShop (Payment.txnRef)
     * @param amount   số tiền (VND)
     * @param clientIp IP của khách
     */
    String buildPaymentUrl(String txnRef, BigDecimal amount, String clientIp);

    /** Chữ ký vnp_SecureHash của callback (return / IPN) có đúng không. */
    boolean verify(Map<String, String> params);
}
