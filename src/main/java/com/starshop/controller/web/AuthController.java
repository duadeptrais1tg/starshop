package com.starshop.controller.web;

import com.starshop.dto.LoginRequest;
import com.starshop.exception.AccountNotActivatedException;
import com.starshop.exception.BusinessException;
import com.starshop.security.JwtService;
import com.starshop.security.SafeRedirect;
import com.starshop.security.UserPrincipal;
import com.starshop.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Đăng nhập / đăng xuất. JWT được trả về trong cookie HttpOnly ACCESS_TOKEN.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String LOGIN_VIEW = "auth/login";

    /** Đổi token CSRF khi đăng nhập / đăng xuất (trang kế tiếp tự sinh token mới), tránh dùng lại token cũ. */
    private static final ResponseCookie CLEAR_CSRF_COOKIE =
            ResponseCookie.from("XSRF-TOKEN", "").path("/").maxAge(0).httpOnly(true).build();

    private final AuthService authService;
    private final JwtService jwtService;

    @GetMapping("/login")
    public String loginForm(@RequestParam(required = false) String redirect,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String reset,
                            @AuthenticationPrincipal UserPrincipal currentUser,
                            Model model) {
        if (currentUser != null) {
            // Đã đăng nhập rồi thì không hiện form nữa
            return "redirect:" + SafeRedirect.resolve(redirect, currentUser.getHomePath());
        }
        LoginRequest form = new LoginRequest();
        form.setRedirect(redirect);
        model.addAttribute("form", form);
        if (logout != null) {
            model.addAttribute("message", "Bạn đã đăng xuất.");
        } else if (reset != null) {
            model.addAttribute("message", "Đặt lại mật khẩu thành công, vui lòng đăng nhập bằng mật khẩu mới.");
        }
        return LOGIN_VIEW;
    }

    @PostMapping("/login")
    public String login(@Valid @ModelAttribute("form") LoginRequest form, BindingResult result,
                        HttpServletResponse response, Model model) {
        if (result.hasErrors()) {
            form.setPassword(null);
            return LOGIN_VIEW;
        }
        try {
            AuthService.LoginResult login = authService.login(form.getEmail(), form.getPassword());
            response.addHeader(HttpHeaders.SET_COOKIE, jwtService.createAccessTokenCookie(login.token()).toString());
            response.addHeader(HttpHeaders.SET_COOKIE, CLEAR_CSRF_COOKIE.toString());
            return "redirect:" + SafeRedirect.resolve(form.getRedirect(), login.user().getHomePath());
        } catch (AccountNotActivatedException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("notActivatedEmail", e.getEmail());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        form.setPassword(null);
        return LOGIN_VIEW;
    }

    /** Chỉ nhận POST (có CSRF token): link GET có thể bị trang khác nhúng vào để ép người dùng đăng xuất. */
    @PostMapping("/logout")
    public String logout(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, CLEAR_CSRF_COOKIE.toString());
        return "redirect:/auth/login?logout";
    }
}
