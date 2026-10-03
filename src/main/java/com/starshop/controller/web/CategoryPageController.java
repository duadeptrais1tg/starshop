package com.starshop.controller.web;

import com.starshop.dto.product.CategoryPageInfo;
import com.starshop.dto.product.ProductSort;
import com.starshop.service.ProductCatalogService;
import com.starshop.util.EnumParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sản phẩm theo danh mục (kể cả danh mục con), có sắp xếp và phân trang.
 */
@Controller
@RequiredArgsConstructor
public class CategoryPageController {

    private final ProductCatalogService catalogService;

    @GetMapping("/categories/{slug}")
    public String category(@PathVariable String slug,
                           @RequestParam(required = false) String sort,
                           @RequestParam(defaultValue = "1") int page,
                           Model model) {
        CategoryPageInfo category = catalogService.categoryInfo(slug);
        ProductSort sortOption = EnumParams.parse(ProductSort.class, sort);
        if (sortOption == null) {
            sortOption = ProductSort.BEST_SELLING;
        }
        model.addAttribute("category", category);
        model.addAttribute("page", catalogService.byCategory(category.getId(), sortOption, page - 1));
        model.addAttribute("sort", sortOption.name());
        model.addAttribute("sorts", ProductSort.values());
        return "web/products/category";
    }
}
