package com.starshop.service;

import com.starshop.dto.revenue.RevenueReport;

import java.time.LocalDate;

/**
 * Thống kê doanh thu của shop. Chỉ tính đơn DELIVERED (theo ngày giao); số liệu tổng hợp bằng query ở repository.
 */
public interface VendorRevenueService {

    int DEFAULT_DAYS = 30;
    /** Khoảng dài hơn số ngày này thì biểu đồ gom theo tháng. */
    int DAILY_CHART_MAX_DAYS = 62;
    int MAX_RANGE_DAYS = 731;
    int TOP_PRODUCTS = 5;

    /**
     * @param from ngày bắt đầu (null = 30 ngày gần nhất)
     * @param to   ngày kết thúc, tính cả ngày này (null = hôm nay)
     * @throws com.starshop.exception.NotFoundException vendor chưa có shop đang hoạt động
     */
    RevenueReport report(Long ownerId, LocalDate from, LocalDate to);
}
