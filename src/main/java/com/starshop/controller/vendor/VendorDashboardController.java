package com.starshop.controller.vendor;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang tổng quan khu vực Vendor.
 */
@Controller
@RequestMapping("/vendor")
public class VendorDashboardController {

    @GetMapping
    public String dashboard() {
        return "vendor/dashboard";
    }
}
