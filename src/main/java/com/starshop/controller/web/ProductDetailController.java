package com.starshop.controller.web;

import com.starshop.dto.product.ProductDetailDto;
import com.starshop.security.UserPrincipal;
import com.starshop.service.ProductCatalogService;
import com.starshop.service.ReviewService;
import com.starshop.service.ViewedProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Trang chi tiết sản phẩm. User đã đăng nhập xem thì được lưu vào lịch sử "Đã xem".
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ProductDetailController {

    private static final int RELATED_LIMIT = 8;

    private final ProductCatalogService catalogService;
    private final ReviewService reviewService;
    private final ViewedProductService viewedProductService;

    /**
     * @param page trang danh sách đánh giá (bắt đầu từ 1)
     */
    @GetMapping("/products/{slug}")
    public String detail(@PathVariable String slug,
                         @RequestParam(defaultValue = "1") int page,
                         @AuthenticationPrincipal UserPrincipal user,
                         Model model) {
        ProductDetailDto product = catalogService.getDetail(slug);
        if (user != null) {
            try {
                viewedProductService.record(user.getId(), product.getId());
            } catch (DataAccessException e) {
                // Mở 2 tab cùng lúc có thể đụng ràng buộc unique; lịch sử xem không được làm hỏng trang
                log.debug("Không ghi được lịch sử xem: {}", e.getMessage());
            }
        }
        model.addAttribute("product", product);
        model.addAttribute("page", reviewService.productReviews(product.getId(), page - 1));
        model.addAttribute("ratingSummary", reviewService.summary(product.getId()));
        model.addAttribute("related", catalogService.related(product, RELATED_LIMIT));
        return "web/products/detail";
    }
}
