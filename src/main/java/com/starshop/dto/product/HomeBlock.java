package com.starshop.dto.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * 4 khối sản phẩm trên trang chủ của User. Mỗi khối tối đa {@link #MAX_ITEMS} sản phẩm,
 * hiển thị {@link #PAGE_SIZE} sản phẩm mỗi lần, nút "Xem thêm" tải tiếp qua /api/products.
 */
@Getter
@RequiredArgsConstructor
public enum HomeBlock {
    NEWEST("Mới nhất", "ti-sparkles", "Chưa có sản phẩm",
            Sort.by(Sort.Direction.DESC, "id")),
    BEST_SELLING("Bán chạy", "ti-flame", "Chưa có sản phẩm bán chạy",
            Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "id"))),
    TOP_RATED("Đánh giá cao", "ti-star", "Chưa có sản phẩm nào được đánh giá",
            Sort.by(Sort.Direction.DESC, "ratingAvg").and(Sort.by(Sort.Direction.DESC, "reviewCount"))
                    .and(Sort.by(Sort.Direction.DESC, "id"))),
    MOST_FAVORITED("Yêu thích nhiều nhất", "ti-heart", "Chưa có sản phẩm nào được yêu thích",
            Sort.by(Sort.Direction.DESC, "favoriteCount").and(Sort.by(Sort.Direction.DESC, "id")));

    public static final int PAGE_SIZE = 10;
    public static final int MAX_ITEMS = 20;
    public static final int MAX_PAGES = MAX_ITEMS / PAGE_SIZE;

    private final String label;
    private final String icon;
    private final String emptyText;
    private final Sort sort;

    /** Tên dùng trên URL API, ví dụ "best_selling". */
    public String getKey() {
        return name().toLowerCase();
    }
}
