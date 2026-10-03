package com.starshop.controller.api;

import com.starshop.dto.product.HomeBlock;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.dto.product.ProductListResponse;
import com.starshop.service.ProductCatalogService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductApiControllerTest {

    private final ProductCatalogService catalogService = mock(ProductCatalogService.class);
    private final ProductApiController controller = new ProductApiController(catalogService);

    @Test
    void secondPage_isLastOneBecauseOfTwentyItemCap() {
        // Có 30 sản phẩm nhưng mỗi khối tối đa 20 -> trang 1 (thứ 2) là trang cuối
        when(catalogService.homeBlock(HomeBlock.NEWEST, 1)).thenReturn(page(1, 30));

        ProductListResponse response = controller.list("newest", 1);

        assertThat(response.items()).hasSize(HomeBlock.PAGE_SIZE);
        assertThat(response.hasMore()).isFalse();
    }

    @Test
    void firstPage_hasMoreWhenEnoughProducts() {
        when(catalogService.homeBlock(HomeBlock.BEST_SELLING, 0)).thenReturn(page(0, 30));

        assertThat(controller.list("BEST_SELLING", 0).hasMore()).isTrue();
    }

    @Test
    void unknownType_isBadRequest() {
        assertThatThrownBy(() -> controller.list("hack", 0)).isInstanceOf(ResponseStatusException.class);
    }

    private static PageImpl<ProductCardDto> page(int number, long total) {
        List<ProductCardDto> items = Collections.nCopies(HomeBlock.PAGE_SIZE, ProductCardDto.builder().build());
        return new PageImpl<>(items, PageRequest.of(number, HomeBlock.PAGE_SIZE), total);
    }
}
