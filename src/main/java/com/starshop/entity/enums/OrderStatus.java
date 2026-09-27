package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Trạng thái đơn hàng (hợp đồng chung, không tự ý đổi).
 * <pre>
 * NEW → CONFIRMED → PICKED_UP → SHIPPING → DELIVERED
 * NEW/CONFIRMED → CANCELLED
 * DELIVERED → RETURN_REQUESTED → REFUNDED
 * </pre>
 * Việc chuyển trạng thái chỉ thực hiện trong OrderService.
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    NEW("Chờ xác nhận"),
    CONFIRMED("Đã xác nhận"),
    PICKED_UP("Đã lấy hàng"),
    SHIPPING("Đang giao"),
    DELIVERED("Đã giao"),
    CANCELLED("Đã hủy"),
    RETURN_REQUESTED("Yêu cầu trả hàng"),
    REFUNDED("Đã hoàn tiền");

    private final String label;
}
