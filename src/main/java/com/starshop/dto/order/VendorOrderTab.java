package com.starshop.dto.order;

import com.starshop.entity.enums.OrderStatus;
import lombok.Getter;

import java.util.List;

/**
 * Các tab ở trang đơn hàng của vendor; mỗi tab gồm một hoặc nhiều trạng thái đơn.
 */
@Getter
public enum VendorOrderTab {
    NEW("Chờ xác nhận", OrderStatus.NEW),
    CONFIRMED("Đã xác nhận", OrderStatus.CONFIRMED),
    PICKED_UP("Đã lấy hàng", OrderStatus.PICKED_UP),
    SHIPPING("Đang giao", OrderStatus.SHIPPING),
    DELIVERED("Đã giao", OrderStatus.DELIVERED),
    CANCELLED("Đã hủy", OrderStatus.CANCELLED),
    RETURNS("Trả hàng / hoàn tiền", OrderStatus.RETURN_REQUESTED, OrderStatus.REFUNDED);

    private final String label;
    private final List<OrderStatus> statuses;

    VendorOrderTab(String label, OrderStatus... statuses) {
        this.label = label;
        this.statuses = List.of(statuses);
    }
}
