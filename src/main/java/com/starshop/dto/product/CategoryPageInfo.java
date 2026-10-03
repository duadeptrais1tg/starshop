package com.starshop.dto.product;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Thông tin danh mục ở đầu trang /categories/{slug}: tên, link danh mục cha, các danh mục con.
 */
@Getter
@AllArgsConstructor
public class CategoryPageInfo {

    private final Long id;
    private final String name;
    private final String slug;
    private final String imageUrl;
    /** null nếu là danh mục gốc. */
    private final CategoryLink parent;
    /** Danh mục con đang hiển thị (bấm để xem nhanh). */
    private final List<CategoryLink> children;

    @Getter
    @AllArgsConstructor
    public static class CategoryLink {
        private final String name;
        private final String slug;
    }
}
