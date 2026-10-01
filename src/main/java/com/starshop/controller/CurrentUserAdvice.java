package com.starshop.controller;

import com.starshop.dto.CurrentUser;
import com.starshop.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Đưa "currentUser" vào model của mọi trang để header/sidebar hiển thị tên và menu theo role.
 * Khách (chưa đăng nhập) thì currentUser = null.
 */
@ControllerAdvice
public class CurrentUserAdvice {

    @ModelAttribute("currentUser")
    public CurrentUser currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        return principal == null ? null : new CurrentUser(principal);
    }
}
