package com.starshop.controller;

import com.starshop.dto.CurrentUser;
import com.starshop.security.UserPrincipal;
import com.starshop.service.CartService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Đưa "currentUser" và "cartCount" vào model của mọi trang để header hiển thị tên, menu theo role,
 * số sản phẩm trong giỏ. Khách (chưa đăng nhập) thì currentUser = null, cartCount = 0.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserAdvice {

    private final CartService cartService;

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return principal == null ? null : new CurrentUser(principal);
    }

    @ModelAttribute("cartCount")
    public long cartCount(@AuthenticationPrincipal UserPrincipal principal, HttpServletRequest request) {
        // API JSON không render header -> không cần đếm
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (principal == null || path.startsWith("/api/")) {
            return 0;
        }
        return cartService.countLines(principal.getId());
    }
}
