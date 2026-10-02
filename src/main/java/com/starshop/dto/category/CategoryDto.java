package com.starshop.dto.category;

import lombok.Builder;
import lombok.Getter;

/**
 * Danh mục hiển thị ở trang quản lý (Admin/Manager).
 */
@Getter
@Builder
public class CategoryDto {

    private final Long id;
    private final String name;
    private final String slug;
    private final String imageUrl;
    private final boolean active;
    private final Long parentId;
    private final String parentName;
    private final long productCount;
    private final String createdAt;

    /** Đang có sản phẩm thì không xóa được, chỉ ẩn. */
    public boolean isDeletable() {
        return productCount == 0;
    }
}
