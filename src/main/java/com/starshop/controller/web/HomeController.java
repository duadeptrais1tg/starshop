package com.starshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDateTime;

/**
 * Trang chủ (tạm thời dùng để kiểm tra JSP chạy được).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("serverTime", LocalDateTime.now());
        return "web/index";
    }
}
