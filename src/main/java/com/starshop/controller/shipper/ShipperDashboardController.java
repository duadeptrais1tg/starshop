package com.starshop.controller.shipper;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang tổng quan khu vực Shipper.
 */
@Controller
@RequestMapping("/shipper")
public class ShipperDashboardController {

    @GetMapping
    public String dashboard() {
        return "shipper/dashboard";
    }
}
