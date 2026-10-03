package com.starshop.controller.api;

import com.starshop.dto.product.HomeBlock;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductListResponse;
import com.starshop.service.ProductCatalogService;
import com.starshop.util.EnumParams;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * API công khai cho nút "Xem thêm" ở trang chủ: GET /api/products?type=best_selling&amp;page=1
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductApiController {

    private final ProductCatalogService catalogService;

    /**
     * @param type newest | best_selling | top_rated | most_favorited
     * @param page bắt đầu từ 0
     */
    @GetMapping
    public ProductListResponse list(@RequestParam String type, @RequestParam(defaultValue = "0") int page) {
        HomeBlock block = EnumParams.parse(HomeBlock.class, type);
        if (block == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "type không hợp lệ");
        }
        Page<ProductCardDto> result = catalogService.homeBlock(block, page);
        boolean hasMore = result.hasNext() && result.getNumber() + 1 < HomeBlock.MAX_PAGES;
        return new ProductListResponse(result.getContent(), result.getNumber(), hasMore);
    }
}
