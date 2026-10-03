package com.starshop.service;

import com.starshop.dto.carrier.CarrierDto;
import com.starshop.dto.carrier.CarrierForm;
import org.springframework.data.domain.Page;

/**
 * Quản lý nhà vận chuyển (dùng chung toàn chuỗi).
 */
public interface CarrierService {

    int PAGE_SIZE = 10;

    /**
     * @param active null = tất cả
     * @param page   bắt đầu từ 0
     */
    Page<CarrierDto> search(String keyword, Boolean active, int page);

    CarrierForm getForm(Long id);

    Long create(CarrierForm form);

    void update(Long id, CarrierForm form);

    void toggleActive(Long id);

    /**
     * @throws com.starshop.exception.BusinessException đã có đơn hàng hoặc shipper thuộc nhà vận chuyển
     */
    void delete(Long id);
}
