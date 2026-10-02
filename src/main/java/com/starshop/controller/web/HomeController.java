package com.starshop.controller.web;

import com.starshop.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Trang chủ: sản phẩm bán chạy (đã bán > 10) của tất cả shop, số bán giảm dần.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductCatalogService catalogService;

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("page", catalogService.bestSellers(page - 1));
        return "web/index";
    }
}
