package com.starshop.service;

import com.starshop.dto.product.ProductCardDto;
import org.springframework.data.domain.Page;

/**
 * Lịch sử sản phẩm đã xem của user đã đăng nhập.
 */
public interface ViewedProductService {

    int PAGE_SIZE = 12;

    /** Ghi nhận đã xem: chưa có thì thêm, có rồi thì chỉ cập nhật thời gian (không tạo trùng). */
    void record(Long userId, Long productId);

    /** @param page bắt đầu từ 0; mới xem gần nhất trước */
    Page<ProductCardDto> viewedBy(Long userId, int page);

    void clear(Long userId);
}
