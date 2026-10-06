package com.starshop.controller.web;

import com.starshop.security.SafeRedirect;
import com.starshop.security.UserPrincipal;
import com.starshop.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Trang "Sản phẩm yêu thích" và nút tim dự phòng khi trình duyệt tắt JavaScript.
 */
@Controller
@RequestMapping("/user/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping
    public String favorites(@AuthenticationPrincipal UserPrincipal user,
                            @RequestParam(defaultValue = "1") int page, Model model) {
        model.addAttribute("page", favoriteService.favoritesOf(user.getId(), page - 1));
        return "web/user/favorites";
    }

    /** Không có JavaScript: form ở trang chi tiết gửi thẳng tới đây, xong quay lại trang cũ. */
    @PostMapping("/{productId}")
    public String toggle(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long productId,
                         @RequestParam(required = false) String redirect) {
        favoriteService.toggle(user.getId(), productId);
        return "redirect:" + SafeRedirect.resolve(redirect, "/user/favorites");
    }
}
