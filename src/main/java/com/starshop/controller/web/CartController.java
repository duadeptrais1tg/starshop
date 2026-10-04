package com.starshop.controller.web;

import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

/**
 * Trang giỏ hàng (/cart/** bắt buộc đăng nhập). Thêm / sửa số lượng bằng AJAX ở CartApiController;
 * ở đây là trang giỏ, xóa dòng, và form "Thêm vào giỏ" dự phòng khi trình duyệt tắt JavaScript.
 */
@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private static final Map<String, String> MESSAGES = Map.of(
            "added", "Đã thêm vào giỏ hàng.",
            "removed", "Đã xóa sản phẩm khỏi giỏ.",
            "addFailed", "Không thêm được sản phẩm: sản phẩm đã ngừng bán, hết hàng hoặc vượt số lượng còn lại.");

    private final CartService cartService;

    @GetMapping
    public String cart(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String msg,
                       Model model) {
        model.addAttribute("cart", cartService.getCart(user.getId()));
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("addFailed".equals(msg) ? "error" : "message", MESSAGES.get(msg));
        }
        return "web/cart/cart";
    }

    /** Dự phòng không có JavaScript: form ở trang chi tiết gửi thẳng tới đây. */
    @PostMapping("/items")
    public String add(@AuthenticationPrincipal UserPrincipal user,
                      @RequestParam Long productId, @RequestParam(defaultValue = "1") int quantity) {
        try {
            cartService.addItem(user.getId(), productId, quantity);
            return "redirect:/cart?msg=added";
        } catch (BusinessException e) {
            // Không đưa thông báo lỗi vào URL (tránh bị lợi dụng chèn nội dung tùy ý), chỉ dùng mã cố định
            return "redirect:/cart?msg=addFailed";
        }
    }

    @PostMapping("/items/{id}/delete")
    public String remove(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        cartService.removeItems(user.getId(), List.of(id));
        return "redirect:/cart?msg=removed";
    }

    /** Xóa nhiều dòng đã chọn. */
    @PostMapping("/items/delete")
    public String removeSelected(@AuthenticationPrincipal UserPrincipal user,
                                 @RequestParam(value = "itemIds", required = false) List<Long> itemIds) {
        cartService.removeItems(user.getId(), itemIds);
        return "redirect:/cart?msg=removed";
    }
}
