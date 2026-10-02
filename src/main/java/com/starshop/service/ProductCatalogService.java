package com.starshop.service;

import com.starshop.dto.OptionDto;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductSearchCriteria;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Danh mục sản phẩm cho khách (Guest/User). Chỉ trả về sản phẩm đang bán, của shop đã duyệt,
 * thuộc danh mục đang hiển thị.
 */
public interface ProductCatalogService {

    int PAGE_SIZE = 12;

    /** Ngưỡng "bán chạy" của trang chủ Guest: đã bán nhiều hơn 10. */
    int BEST_SELLER_MIN_SOLD = 10;

    /**
     * Sản phẩm đã bán > 10, sắp xếp số bán giảm dần (trang chủ Guest).
     *
     * @param page bắt đầu từ 0
     */
    Page<ProductCardDto> bestSellers(int page);

    /** Tìm kiếm / lọc / sắp xếp / phân trang. Criteria đã được normalize. */
    Page<ProductCardDto> search(ProductSearchCriteria criteria);

    /** Shop đang hoạt động (cho ô lọc theo shop). */
    List<OptionDto> activeShops();
}
