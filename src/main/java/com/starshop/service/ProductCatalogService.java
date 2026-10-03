package com.starshop.service;

import com.starshop.dto.OptionDto;
import com.starshop.dto.product.CategoryPageInfo;
import com.starshop.dto.product.HomeBlock;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductDetailDto;
import com.starshop.dto.product.ProductSearchCriteria;
import com.starshop.dto.product.ProductSort;
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

    /**
     * Một khối của trang chủ User (tối đa 20 sản phẩm, mỗi lần 10).
     *
     * @param page bắt đầu từ 0; vượt quá giới hạn 20 sản phẩm thì trả về trang rỗng
     */
    Page<ProductCardDto> homeBlock(HomeBlock block, int page);

    /** @throws com.starshop.exception.NotFoundException danh mục không tồn tại hoặc đang ẩn */
    CategoryPageInfo categoryInfo(String slug);

    /** Sản phẩm của danh mục (kể cả danh mục con). */
    Page<ProductCardDto> byCategory(Long categoryId, ProductSort sort, int page);

    /** @throws com.starshop.exception.NotFoundException không có hoặc khách không được xem */
    ProductDetailDto getDetail(String slug);

    /** Sản phẩm cùng danh mục, bán chạy trước, bỏ chính nó. */
    List<ProductCardDto> related(ProductDetailDto product, int limit);
}
