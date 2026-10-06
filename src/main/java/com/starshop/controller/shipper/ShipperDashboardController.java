package com.starshop.controller.shipper;

import com.starshop.security.UserPrincipal;
import com.starshop.service.ShipperOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Trang tổng quan và thống kê của Shipper.
 */
@Controller
@RequestMapping("/shipper")
@RequiredArgsConstructor
public class ShipperDashboardController {

    private final ShipperOrderService orderService;

    @GetMapping
    public String dashboard(@AuthenticationPrincipal UserPrincipal user, Model model) {
        model.addAttribute("stats", orderService.stats(user.getId()));
        return "shipper/dashboard";
    }

    /** Số đơn được phân công, đang giao, đã giao, tỉ lệ thành công theo tháng. */
    @GetMapping("/stats")
    public String stats(@AuthenticationPrincipal UserPrincipal user, Model model) {
        model.addAttribute("stats", orderService.stats(user.getId()));
        return "shipper/stats";
    }
}
