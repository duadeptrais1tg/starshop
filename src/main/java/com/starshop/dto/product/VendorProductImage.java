package com.starshop.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Ảnh của sản phẩm ở form sửa (để chọn ảnh đại diện / xóa).
 */
@Getter
@AllArgsConstructor
public class VendorProductImage {

    private final Long id;
    private final String url;
    private final boolean thumbnail;
}
