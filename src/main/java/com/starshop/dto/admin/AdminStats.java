package com.starshop.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Số liệu tổng quan cho dashboard Admin.
 */
@Getter
@AllArgsConstructor
public class AdminStats {

    private final long totalUsers;
    private final long approvedShops;
    private final long pendingShops;
    private final long totalOrders;
    /** Doanh thu toàn sàn = tổng tiền các đơn đã giao thành công. */
    private final BigDecimal revenue;
}
