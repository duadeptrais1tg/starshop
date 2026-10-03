package com.starshop.controller.web;

import com.starshop.security.UserPrincipal;
import com.starshop.service.ViewedProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Sản phẩm đã xem (/user/** bắt buộc đăng nhập). userId lấy từ người đang đăng nhập, không lấy từ URL,
 * nên không xem / xóa được lịch sử của người khác.
 */
@Controller
@RequestMapping("/user/viewed")
@RequiredArgsConstructor
public class ViewedProductController {

    private final ViewedProductService viewedProductService;

    @GetMapping
    public String list(@RequestParam(defaultValue = "1") int page,
                       @AuthenticationPrincipal UserPrincipal user, Model model) {
        model.addAttribute("page", viewedProductService.viewedBy(user.getId(), page - 1));
        return "web/user/viewed";
    }

    @PostMapping("/clear")
    public String clear(@AuthenticationPrincipal UserPrincipal user) {
        viewedProductService.clear(user.getId());
        return "redirect:/user/viewed";
    }
}
