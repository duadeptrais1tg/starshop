package com.starshop.dto.product;

import java.util.List;

/**
 * Kết quả JSON của /api/products (nút "Xem thêm").
 *
 * @param items   sản phẩm của trang này
 * @param page    trang vừa trả về (bắt đầu từ 0)
 * @param hasMore còn trang sau không (đã tính giới hạn tối đa 20 sản phẩm mỗi khối)
 */
public record ProductListResponse(List<ProductCardDto> items, int page, boolean hasMore) {
}
