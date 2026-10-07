package com.starshop.dto.revenue;

import com.starshop.entity.enums.OrderStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Báo cáo doanh thu của shop trong một khoảng ngày (chỉ đơn đã giao, tính theo ngày giao).
 */
@Getter
@Builder
public class RevenueReport {

    private final LocalDate from;
    private final LocalDate to;
    private final String fromLabel;
    private final String toLabel;

    /** Số đơn đã giao trong khoảng. */
    private final long deliveredOrders;
    /** Doanh thu = tiền hàng - giảm giá sản phẩm. */
    private final BigDecimal revenue;
    /** Chiết khấu app. */
    private final BigDecimal commission;

    /** Số đơn đặt trong khoảng theo từng trạng thái (đủ mọi trạng thái, theo thứ tự luồng đơn). */
    private final Map<OrderStatus, Long> statusCounts;

    /** true = biểu đồ theo tháng (khoảng dài), false = theo ngày. */
    private final boolean monthly;
    private final List<String> chartLabels;
    private final List<BigDecimal> chartRevenue;
    private final List<BigDecimal> chartNet;

    private final List<TopProduct> topProducts;

    /** Thực nhận = doanh thu - chiết khấu app (làm tròn đồng). */
    public BigDecimal getNet() {
        return revenue.subtract(commission).setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal getCommissionRounded() {
        return commission.setScale(0, RoundingMode.HALF_UP);
    }

    public long getTotalOrders() {
        return statusCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    @Getter
    @Builder
    public static class TopProduct {
        private final int rank;
        private final Long productId;
        private final String name;
        private final String slug;
        private final String imageUrl;
        private final long quantity;
        private final BigDecimal revenue;
    }
}
