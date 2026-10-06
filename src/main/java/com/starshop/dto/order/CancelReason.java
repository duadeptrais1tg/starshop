package com.starshop.dto.order;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Lý do khách hủy đơn (bắt buộc chọn). OTHER cần nhập thêm nội dung.
 */
@Getter
@RequiredArgsConstructor
public enum CancelReason {
    CHANGE_ADDRESS("Muốn thay đổi địa chỉ / thời gian nhận hàng"),
    CHANGE_PRODUCT("Muốn thay đổi sản phẩm hoặc số lượng"),
    CHANGE_COUPON("Muốn áp dụng mã giảm giá khác"),
    BETTER_PRICE("Tìm thấy chỗ khác giá tốt hơn"),
    NO_LONGER_NEEDED("Không còn nhu cầu"),
    OTHER("Lý do khác");

    private final String label;
}
