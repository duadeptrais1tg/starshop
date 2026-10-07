package com.starshop.dto.revenue;

import java.math.BigDecimal;

/**
 * Kết quả các query tổng hợp doanh thu (Spring Data interface projection, tên khớp alias trong query).
 * Doanh thu của shop = tiền hàng - giảm giá sản phẩm (phí vận chuyển không tính cho shop).
 */
public final class RevenueProjections {

    private RevenueProjections() {
    }

    /** Tổng của các đơn đã giao trong khoảng thời gian. */
    public interface Totals {
        long getOrderCount();

        BigDecimal getRevenue();

        /** Chiết khấu app = doanh thu x % chiết khấu lưu trên từng đơn. */
        BigDecimal getCommission();
    }

    /** Một điểm trên biểu đồ (theo ngày "yyyy-MM-dd" hoặc theo tháng "yyyy-MM"). */
    public interface Point {
        String getPeriod();

        BigDecimal getRevenue();

        BigDecimal getCommission();

        long getOrders();
    }

    /** Sản phẩm bán chạy. */
    public interface TopProduct {
        Long getProductId();

        String getName();

        String getSlug();

        long getQuantity();

        BigDecimal getRevenue();
    }
}
