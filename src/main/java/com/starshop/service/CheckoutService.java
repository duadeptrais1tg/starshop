package com.starshop.service;

import com.starshop.dto.checkout.CheckoutRequest;
import com.starshop.dto.checkout.CheckoutView;
import com.starshop.dto.checkout.OrderSuccessView;

/**
 * Đặt hàng từ giỏ: xem trước (tính tiền, áp mã) và tạo đơn. Mọi số tiền tính lại ở server.
 */
public interface CheckoutService {

    /** Số dòng giỏ tối đa trong một lần đặt hàng. */
    int MAX_ITEMS = 100;

    /**
     * Dữ liệu trang checkout. Lỗi (mã không hợp lệ, hết hàng, chưa có địa chỉ...) nằm trong
     * {@link CheckoutView#getErrors()} chứ không ném ra, để khách sửa ngay trên trang.
     *
     * @throws com.starshop.exception.BusinessException không còn sản phẩm nào được chọn trong giỏ
     */
    CheckoutView preview(Long userId, CheckoutRequest request);

    /**
     * Tạo đơn trong một transaction: tách đơn theo shop, trừ tồn kho (không để âm), ghi lượt dùng mã,
     * xóa sản phẩm đã đặt khỏi giỏ, trạng thái NEW.
     *
     * @return mã giao dịch (txnRef) của lần thanh toán, dùng cho trang đặt hàng thành công
     * @throws com.starshop.exception.BusinessException có điều kiện không thỏa (toàn bộ đơn không được tạo)
     */
    String placeOrder(Long userId, CheckoutRequest request);

    /** @throws com.starshop.exception.NotFoundException không có đơn của user với mã giao dịch này */
    OrderSuccessView success(Long userId, String txnRef);
}
