package com.starshop.service;

import com.starshop.entity.enums.OrderStatus;

/**
 * Nơi DUY NHẤT chuyển trạng thái đơn hàng (theo luồng trong OrderStatus):
 * <pre>
 * NEW → CONFIRMED → PICKED_UP → SHIPPING → DELIVERED
 * NEW/CONFIRMED → CANCELLED
 * DELIVERED → RETURN_REQUESTED → REFUNDED
 * </pre>
 * Các chức năng khác (hủy đơn, vendor xác nhận, shipper giao, thanh toán thất bại...) đều gọi service này.
 */
public interface OrderService {

    /** Chuyển trạng thái có hợp lệ theo luồng không. */
    boolean canTransition(OrderStatus from, OrderStatus to);

    /**
     * Chuyển trạng thái đơn, ghi lịch sử. Khi chuyển sang CANCELLED: hoàn lại tồn kho và lượt dùng mã giảm giá.
     * <p>
     * Lưu ý: trả lượt mã dùng câu UPDATE có clear persistence context, nên entity đã nạp trước khi gọi
     * có thể bị tách khỏi context; bên gọi cần nạp lại entity nếu còn sửa tiếp.
     *
     * @param changedByUserId người thực hiện, null = hệ thống (ví dụ callback thanh toán)
     * @throws com.starshop.exception.NotFoundException  không có đơn
     * @throws com.starshop.exception.BusinessException chuyển trạng thái không hợp lệ
     */
    void changeStatus(Long orderId, OrderStatus to, Long changedByUserId, String note);
}
