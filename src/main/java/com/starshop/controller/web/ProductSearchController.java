package com.starshop.controller.web;

import com.starshop.dto.product.ProductSearchCriteria;
import com.starshop.dto.product.ProductSort;
import com.starshop.service.CategoryService;
import com.starshop.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Tìm kiếm / lọc sản phẩm. Mọi tham số nằm trên URL (form GET) nên có thể chia sẻ link, bấm quay lại.
 */
@Controller
@RequiredArgsConstructor
public class ProductSearchController {

    private final ProductCatalogService catalogService;
    private final CategoryService categoryService;

    @GetMapping("/products/search")
    public String search(@ModelAttribute("criteria") ProductSearchCriteria criteria,
                         BindingResult bindingResult, Model model) {
        // Có BindingResult thì tham số sai kiểu (minPrice=abc) chỉ bị bỏ qua (null) thay vì lỗi 400
        criteria.normalize();
        model.addAttribute("page", catalogService.search(criteria));
        model.addAttribute("categories", categoryService.activeOptions());
        model.addAttribute("shops", catalogService.activeShops());
        model.addAttribute("sorts", ProductSort.values());
        return "web/products/search";
    }
}
