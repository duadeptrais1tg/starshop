package com.starshop.dto.product;

import com.starshop.util.EnumParams;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

/**
 * Tham số tìm kiếm / lọc sản phẩm, lấy thẳng từ URL (?keyword=&categoryId=&minPrice=...).
 * Giá trị sai (người dùng tự sửa URL) được bỏ qua hoặc sửa lại bằng {@link #normalize()}, không báo lỗi.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductSearchCriteria {

    public static final int MAX_KEYWORD_LENGTH = 100;

    private String keyword;
    private Long categoryId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private Long shopId;
    /** Từ bao nhiêu sao trở lên (1–5). */
    private Integer rating;
    /** Tên enum ProductSort, mặc định NEWEST. */
    private String sort;
    /** Trang, đánh số từ 1 trên URL. */
    private int page = 1;

    public ProductSearchCriteria normalize() {
        keyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        if (keyword != null && keyword.length() > MAX_KEYWORD_LENGTH) {
            keyword = keyword.substring(0, MAX_KEYWORD_LENGTH);
        }
        if (minPrice != null && minPrice.signum() < 0) {
            minPrice = null;
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            maxPrice = null;
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            BigDecimal tmp = minPrice;
            minPrice = maxPrice;
            maxPrice = tmp;
        }
        if (rating != null && (rating < 1 || rating > 5)) {
            rating = null;
        }
        if (page < 1) {
            page = 1;
        }
        sort = getSortOption().name();
        return this;
    }

    public ProductSort getSortOption() {
        ProductSort parsed = EnumParams.parse(ProductSort.class, sort);
        return parsed == null ? ProductSort.NEWEST : parsed;
    }

    /** Có đang lọc gì không (để hiện nút "Xóa bộ lọc"). */
    public boolean isFiltered() {
        return keyword != null || categoryId != null || minPrice != null || maxPrice != null
                || shopId != null || rating != null;
    }
}
