package com.starshop.controller.vendor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.starshop.dto.revenue.RevenueReport;
import com.starshop.security.UserPrincipal;
import com.starshop.service.VendorRevenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Thống kê doanh thu của shop: lọc theo khoảng ngày, tổng doanh thu / chiết khấu / thực nhận,
 * số đơn theo trạng thái, biểu đồ (Chart.js), top 5 sản phẩm bán chạy.
 */
@Controller
@RequestMapping("/vendor/revenue")
@RequiredArgsConstructor
public class VendorRevenueController {

    private final VendorRevenueService revenueService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @GetMapping
    public String revenue(@AuthenticationPrincipal UserPrincipal user,
                          @RequestParam(required = false) String from,
                          @RequestParam(required = false) String to,
                          Model model) throws JsonProcessingException {
        RevenueReport report = revenueService.report(user.getId(), parseDate(from), parseDate(to));
        model.addAttribute("report", report);
        // Dữ liệu biểu đồ dạng JSON (nhãn là ngày/tháng, giá trị là số) cho Chart.js
        model.addAttribute("chartJson", objectMapper.writeValueAsString(Map.of(
                "labels", report.getChartLabels(),
                "revenue", report.getChartRevenue(),
                "net", report.getChartNet())));

        LocalDate today = LocalDate.now(clock);
        model.addAttribute("today", today);
        model.addAttribute("last7", today.minusDays(6));
        model.addAttribute("last30", today.minusDays(29));
        model.addAttribute("monthStart", today.withDayOfMonth(1));
        model.addAttribute("yearStart", today.withDayOfYear(1));
        return "vendor/revenue";
    }

    private static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
