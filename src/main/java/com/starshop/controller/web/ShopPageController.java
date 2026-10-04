package com.starshop.controller.web;

import com.starshop.dto.product.ProductSearchCriteria;
import com.starshop.dto.product.ProductSort;
import com.starshop.dto.shop.ShopPageDto;
import com.starshop.service.ProductCatalogService;
import com.starshop.service.ShopService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Trang shop công khai /shop/{slug}: banner, thông tin, đánh giá trung bình và sản phẩm của shop
 * (tìm trong shop, sắp xếp, phân trang).
 */
@Controller
@RequiredArgsConstructor
public class ShopPageController {

    private final ShopService shopService;
    private final ProductCatalogService catalogService;

    @GetMapping("/shop/{slug}")
    public String shop(@PathVariable String slug,
                       @ModelAttribute("criteria") ProductSearchCriteria criteria, BindingResult bindingResult,
                       Model model) {
        ShopPageDto shop = shopService.getPublicPage(slug);
        // Chỉ giữ từ khóa + sắp xếp + trang; luôn lọc theo shop này (bỏ qua các tham số lọc khác trên URL)
        criteria.setCategoryId(null);
        criteria.setMinPrice(null);
        criteria.setMaxPrice(null);
        criteria.setRating(null);
        criteria.setShopId(shop.getId());
        criteria.normalize();
        model.addAttribute("shop", shop);
        model.addAttribute("page", catalogService.search(criteria));
        model.addAttribute("sorts", ProductSort.values());
        return "web/shop/page";
    }
}
