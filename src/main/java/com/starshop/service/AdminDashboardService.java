package com.starshop.service;

import com.starshop.dto.admin.AdminStats;
import com.starshop.dto.admin.ShopAdminDto;

import java.util.List;

/**
 * Số liệu tổng quan cho trang /admin.
 */
public interface AdminDashboardService {

    AdminStats getStats();

    /** Tối đa 5 shop chờ duyệt lâu nhất. */
    List<ShopAdminDto> oldestPendingShops();
}
