package com.starshop.controller.web;

import com.starshop.dto.product.HomeBlock;
import com.starshop.dto.product.ProductCardDto;
import com.starshop.security.UserPrincipal;
import com.starshop.service.ProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Trang chủ.
 * <ul>
 *   <li>Guest: sản phẩm đã bán > 10 của tất cả shop, số bán giảm dần (có phân trang).</li>
 *   <li>User đã đăng nhập: 4 khối mới nhất / bán chạy / đánh giá cao / yêu thích nhiều nhất,
 *       mỗi khối tối đa 20 sản phẩm, nút "Xem thêm" tải tiếp qua /api/products.</li>
 * </ul>
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductCatalogService catalogService;

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "1") int page,
                        @AuthenticationPrincipal UserPrincipal user,
                        Model model) {
        if (user != null) {
            Map<HomeBlock, Page<ProductCardDto>> blocks = new LinkedHashMap<>();
            for (HomeBlock block : HomeBlock.values()) {
                blocks.put(block, catalogService.homeBlock(block, 0));
            }
            model.addAttribute("blocks", blocks);
            model.addAttribute("maxPages", HomeBlock.MAX_PAGES);
            return "web/home-user";
        }
        model.addAttribute("page", catalogService.bestSellers(page - 1));
        return "web/index";
    }
}
