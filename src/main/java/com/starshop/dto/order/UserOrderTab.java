package com.starshop.dto.order;

import com.starshop.entity.enums.OrderStatus;
import lombok.Getter;

import java.util.List;

/**
 * Các tab ở trang "Đơn hàng của tôi".
 */
@Getter
public enum UserOrderTab {
    NEW("Chờ xác nhận", OrderStatus.NEW),
    CONFIRMED("Đã xác nhận", OrderStatus.CONFIRMED),
    SHIPPING("Đang giao", OrderStatus.PICKED_UP, OrderStatus.SHIPPING),
    DELIVERED("Đã giao", OrderStatus.DELIVERED),
    CANCELLED("Đã hủy", OrderStatus.CANCELLED),
    RETURNS("Trả hàng / hoàn tiền", OrderStatus.RETURN_REQUESTED, OrderStatus.REFUNDED);

    private final String label;
    private final List<OrderStatus> statuses;

    UserOrderTab(String label, OrderStatus... statuses) {
        this.label = label;
        this.statuses = List.of(statuses);
    }
}
