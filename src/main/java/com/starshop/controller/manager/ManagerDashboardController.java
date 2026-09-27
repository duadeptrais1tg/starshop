package com.starshop.controller.manager;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang tổng quan khu vực Manager.
 */
@Controller
@RequestMapping("/manager")
public class ManagerDashboardController {

    @GetMapping
    public String dashboard() {
        return "manager/dashboard";
    }
}
