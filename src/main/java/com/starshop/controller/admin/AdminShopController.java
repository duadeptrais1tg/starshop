package com.starshop.controller.admin;

import com.starshop.entity.enums.ShopStatus;
import com.starshop.exception.BusinessException;
import com.starshop.service.AdminShopService;
import com.starshop.service.AdminUserService;
import com.starshop.util.EnumParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Admin duyệt / từ chối / đình chỉ / mở lại shop của vendor.
 */
@Controller
@RequestMapping("/admin/shops")
@RequiredArgsConstructor
public class AdminShopController {

    private static final Map<String, String> MESSAGES = Map.of(
            "approved", "Đã duyệt shop. Chủ shop đã được cấp quyền Người bán.",
            "rejected", "Đã từ chối shop.",
            "suspended", "Đã đình chỉ shop.",
            "reactivated", "Đã mở lại shop.");

    private final AdminShopService shopService;
    private final AdminUserService userService;

    /**
     * @param status mặc định PENDING (việc cần xử lý); "ALL" = tất cả
     */
    @GetMapping
    public String list(@RequestParam(defaultValue = "PENDING") String status,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        ShopStatus filter = EnumParams.parse(ShopStatus.class, status);
        model.addAttribute("page", shopService.search(filter, keyword, page - 1));
        model.addAttribute("counts", shopService.countByStatus());
        model.addAttribute("selectedStatus", filter == null ? "ALL" : filter.name());
        model.addAttribute("statuses", ShopStatus.values());
        model.addAttribute("keyword", keyword);
        return "admin/shops/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        return showDetail(id, model);
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, @RequestParam(required = false) Long storeId, Model model) {
        return act(id, "approved", () -> shopService.approve(id, storeId), model);
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id, @RequestParam(required = false) String reason, Model model) {
        return act(id, "rejected", () -> shopService.reject(id, reason), model);
    }

    @PostMapping("/{id}/suspend")
    public String suspend(@PathVariable Long id, @RequestParam(required = false) String reason, Model model) {
        return act(id, "suspended", () -> shopService.suspend(id, reason), model);
    }

    @PostMapping("/{id}/reactivate")
    public String reactivate(@PathVariable Long id, Model model) {
        return act(id, "reactivated", () -> shopService.reactivate(id), model);
    }

    private String act(Long id, String successKey, Runnable action, Model model) {
        try {
            action.run();
            return "redirect:/admin/shops/" + id + "?msg=" + successKey;
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showDetail(id, model);
        }
    }

    private String showDetail(Long id, Model model) {
        model.addAttribute("shop", shopService.get(id));
        model.addAttribute("stores", userService.activeStores());
        return "admin/shops/detail";
    }
}
