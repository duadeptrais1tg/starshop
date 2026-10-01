package com.starshop.controller.web;

import com.starshop.dto.ResetPasswordRequest;
import com.starshop.exception.BusinessException;
import com.starshop.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Quên mật khẩu: nhập email -> nhận OTP -> nhập OTP + mật khẩu mới.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String FORGOT_VIEW = "auth/forgot-password";
    private static final String RESET_VIEW = "auth/reset-password";
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final long RESEND_WAIT_SECONDS = 60;

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String forgotForm() {
        return FORGOT_VIEW;
    }

    @PostMapping("/forgot-password")
    public String requestReset(@RequestParam(required = false) String email, Model model) {
        if (!StringUtils.hasText(email) || !EMAIL.matcher(email.trim()).matches()) {
            model.addAttribute("error", "Vui lòng nhập email hợp lệ.");
            model.addAttribute("email", email);
            return FORGOT_VIEW;
        }
        passwordResetService.requestReset(email);
        // Luôn chuyển sang bước nhập mã, dù email có tồn tại hay không (không cho dò email)
        return "redirect:/auth/reset-password?email="
                + URLEncoder.encode(email.trim().toLowerCase(Locale.ROOT), StandardCharsets.UTF_8) + "&sent";
    }

    @GetMapping("/reset-password")
    public String resetForm(@RequestParam(required = false) String email,
                            @RequestParam(required = false) String sent,
                            Model model) {
        if (!StringUtils.hasText(email)) {
            return "redirect:/auth/forgot-password";
        }
        ResetPasswordRequest form = new ResetPasswordRequest();
        form.setEmail(email.trim());
        model.addAttribute("form", form);
        if (sent != null) {
            model.addAttribute("message", "Nếu email đã đăng ký tài khoản, mã OTP đã được gửi tới hộp thư.");
            // Đếm ngược như nhau với mọi email để không lộ email nào có tài khoản
            model.addAttribute("resendWaitSeconds", RESEND_WAIT_SECONDS);
        }
        return RESET_VIEW;
    }

    @PostMapping("/reset-password")
    public String reset(@Valid @ModelAttribute("form") ResetPasswordRequest form, BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                passwordResetService.resetPassword(form);
                return "redirect:/auth/login?reset";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        form.setPassword(null);
        form.setConfirmPassword(null);
        return RESET_VIEW;
    }
}
