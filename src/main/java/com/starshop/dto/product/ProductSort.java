package com.starshop.dto.product;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;

/**
 * Cách sắp xếp ở trang tìm kiếm. Luôn kèm id giảm dần để thứ tự ổn định khi phân trang.
 */
@Getter
@RequiredArgsConstructor
public enum ProductSort {
    NEWEST("Mới nhất", Sort.by(Sort.Direction.DESC, "id")),
    PRICE_ASC("Giá tăng dần", Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "id"))),
    PRICE_DESC("Giá giảm dần", Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "id"))),
    BEST_SELLING("Bán chạy", Sort.by(Sort.Direction.DESC, "soldCount").and(Sort.by(Sort.Direction.DESC, "id")));

    private final String label;
    private final Sort sort;
}
