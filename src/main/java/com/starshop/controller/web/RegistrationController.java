package com.starshop.controller.web;

import com.starshop.dto.RegisterRequest;
import com.starshop.exception.BusinessException;
import com.starshop.exception.EmailAlreadyExistsException;
import com.starshop.service.UserRegistrationService;
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

/**
 * Đăng ký tài khoản và kích hoạt bằng OTP.
 * Không dùng flash attribute (cần session) vì ứng dụng stateless: thông báo truyền qua tham số URL.
 */
@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class RegistrationController {

    private static final String REGISTER_VIEW = "auth/register";
    private static final String VERIFY_VIEW = "auth/verify-otp";

    private final UserRegistrationService registrationService;

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new RegisterRequest());
        return REGISTER_VIEW;
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterRequest form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return REGISTER_VIEW;
        }
        try {
            String email = registrationService.register(form);
            return "redirect:" + verifyUrl(email, "sent");
        } catch (EmailAlreadyExistsException e) {
            result.rejectValue("email", "duplicate", e.getMessage());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
        }
        return REGISTER_VIEW;
    }

    @GetMapping("/verify-otp")
    public String verifyForm(@RequestParam(required = false) String email,
                             @RequestParam(required = false) String sent,
                             @RequestParam(required = false) String resent,
                             Model model) {
        if (!StringUtils.hasText(email)) {
            return "redirect:/auth/register";
        }
        if (sent != null) {
            model.addAttribute("message", "Mã OTP đã được gửi tới email của bạn.");
        } else if (resent != null) {
            model.addAttribute("message", "Đã gửi lại mã OTP mới, vui lòng kiểm tra email.");
        }
        return showVerify(email, model);
    }

    @PostMapping("/verify-otp")
    public String verify(@RequestParam String email, @RequestParam(required = false) String code, Model model) {
        if (code == null || !code.trim().matches("\\d{6}")) {
            model.addAttribute("error", "Mã OTP gồm 6 chữ số.");
            return showVerify(email, model);
        }
        try {
            registrationService.activate(email, code.trim());
            return "redirect:/auth/activated";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showVerify(email, model);
        }
    }

    @PostMapping("/resend-otp")
    public String resend(@RequestParam String email, Model model) {
        try {
            registrationService.resendActivationCode(email);
            return "redirect:" + verifyUrl(email, "resent");
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showVerify(email, model);
        }
    }

    @GetMapping("/activated")
    public String activated() {
        return "auth/activated";
    }

    private String showVerify(String email, Model model) {
        model.addAttribute("email", email.trim());
        model.addAttribute("resendWaitSeconds", registrationService.secondsUntilResend(email));
        return VERIFY_VIEW;
    }

    private static String verifyUrl(String email, String flag) {
        // URLEncoder mã hóa cả dấu "+" (email dạng a+b@gmail.com), UriComponentsBuilder thì không
        return "/auth/verify-otp?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&" + flag;
    }
}
