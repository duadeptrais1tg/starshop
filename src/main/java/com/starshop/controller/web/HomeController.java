package com.starshop.controller.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang chủ (nội dung chi tiết làm ở chức năng trang chủ Guest/User).
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String index() {
        return "web/index";
    }
}
