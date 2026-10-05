package com.starshop.service;

import java.util.List;
import java.util.Map;

/**
 * Thanh toán online (VNPAY): tạo URL thanh toán, xử lý callback, hủy giao dịch quá hạn.
 */
public interface PaymentService {

    /**
     * URL chuyển khách sang VNPAY cho lần thanh toán vừa tạo khi đặt hàng.
     *
     * @throws com.starshop.exception.NotFoundException  không phải giao dịch của user
     * @throws com.starshop.exception.BusinessException giao dịch đã xử lý / đã quá hạn
     */
    String createVnpayUrl(Long userId, String txnRef, String clientIp);

    /**
     * Xử lý callback của VNPAY (dùng chung cho IPN và return URL, chỉ xử lý 1 lần cho mỗi giao dịch):
     * kiểm tra chữ ký, mã giao dịch, số tiền. Thành công -> Payment PAID. Thất bại / khách hủy -> Payment
     * FAILED / CANCELLED, hủy các đơn (hoàn tồn kho, trả lượt mã) và trả sản phẩm lại giỏ hàng.
     */
    VnpayCallbackResult handleVnpayCallback(Map<String, String> params);

    /** Mã giao dịch VNPAY còn chờ nhưng đã quá hạn thanh toán (khách bỏ dở). */
    List<String> staleVnpayTxnRefs();

    /** Hủy một giao dịch quá hạn (nếu vẫn còn chờ). */
    void expire(String txnRef);

    /**
     * Kết quả xử lý callback.
     *
     * @param rspCode mã trả lời cho IPN của VNPAY (00 = đã ghi nhận, 01 = không có giao dịch, 02 = đã xử lý,
     *                04 = sai số tiền, 97 = sai chữ ký, 99 = lỗi khác)
     * @param txnRef  mã giao dịch (null nếu chữ ký sai)
     * @param paid    giao dịch đã thanh toán thành công
     */
    record VnpayCallbackResult(String rspCode, String message, String txnRef, boolean paid) {
    }
}
