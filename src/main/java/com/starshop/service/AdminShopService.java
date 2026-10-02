package com.starshop.service;

import com.starshop.dto.admin.ShopAdminDto;
import com.starshop.entity.enums.ShopStatus;
import org.springframework.data.domain.Page;

import java.util.Map;

/**
 * Duyệt / từ chối / đình chỉ shop (Admin).
 * <pre>
 * PENDING  -> APPROVED (gán chi nhánh, chủ shop được cấp role VENDOR) | REJECTED (có lý do)
 * APPROVED -> SUSPENDED (có lý do)
 * SUSPENDED -> APPROVED (mở lại)
 * </pre>
 */
public interface AdminShopService {

    int PAGE_SIZE = 10;

    Page<ShopAdminDto> search(ShopStatus status, String keyword, int page);

    /** Số shop theo từng trạng thái (hiển thị trên tab). */
    Map<ShopStatus, Long> countByStatus();

    ShopAdminDto get(Long id);

    void approve(Long shopId, Long storeId);

    void reject(Long shopId, String reason);

    void suspend(Long shopId, String reason);

    void reactivate(Long shopId);
}
