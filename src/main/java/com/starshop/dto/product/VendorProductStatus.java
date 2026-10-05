package com.starshop.dto.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bộ lọc trạng thái ở danh sách sản phẩm của vendor.
 */
@Getter
@RequiredArgsConstructor
public enum VendorProductStatus {
    ACTIVE("Đang bán"),
    INACTIVE("Ngừng bán"),
    OUT_OF_STOCK("Hết hàng");

    private final String label;
}
