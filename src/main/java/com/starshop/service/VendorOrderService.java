package com.starshop.service;

import com.starshop.dto.order.VendorOrderDetail;
import com.starshop.dto.order.VendorOrderRow;
import com.starshop.dto.order.VendorOrderTab;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.Map;

/**
 * Vendor quản lý đơn của shop mình. Mọi thao tác kiểm tra đơn thuộc shop của vendor (đơn shop khác -> không tìm thấy)
 * và chuyển trạng thái qua OrderService (sai luồng -> BusinessException).
 * Shop chỉ thấy đơn COD hoặc đơn online đã thanh toán.
 */
public interface VendorOrderService {

    int PAGE_SIZE = 10;

    /** @param page bắt đầu từ 0 */
    Page<VendorOrderRow> search(Long ownerId, VendorOrderTab tab, String code, LocalDate from, LocalDate to, int page);

    /** Số đơn của từng tab (theo cùng bộ lọc mã / ngày). */
    Map<VendorOrderTab, Long> countByTab(Long ownerId, String code, LocalDate from, LocalDate to);

    VendorOrderDetail detail(Long ownerId, Long orderId);

    /** NEW -> CONFIRMED. */
    void confirm(Long ownerId, Long orderId);

    /** NEW / CONFIRMED -> CANCELLED (bắt buộc lý do). OrderService hoàn tồn kho và lượt mã. */
    void cancel(Long ownerId, Long orderId, String reason);

    /**
     * Giao cho shipper thuộc nhà vận chuyển của đơn: tạo ShipperAssignment, CONFIRMED -> PICKED_UP.
     *
     * @throws com.starshop.exception.BusinessException shipper không hợp lệ / đơn không ở trạng thái đã xác nhận
     */
    void assignShipper(Long ownerId, Long orderId, Long shipperId);

    /** Chấp nhận trả hàng: RETURN_REQUESTED -> REFUNDED. */
    void approveReturn(Long ownerId, Long orderId);

    /** Từ chối trả hàng (bắt buộc lý do): RETURN_REQUESTED -> DELIVERED. */
    void rejectReturn(Long ownerId, Long orderId, String reason);
}
