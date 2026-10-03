package com.starshop.controller.admin;

import com.starshop.dto.commission.CommissionForm;
import com.starshop.exception.BusinessException;
import com.starshop.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Admin quản lý chiết khấu app: mức mặc định toàn sàn và mức riêng từng shop.
 */
@Controller
@RequestMapping("/admin/commissions")
@RequiredArgsConstructor
public class AdminCommissionController {

    private static final String INDEX_VIEW = "admin/commissions/index";
    private static final String SHOP_VIEW = "admin/commissions/shop";
    private static final Map<String, String> MESSAGES = Map.of(
            "added", "Đã thêm mức chiết khấu.",
            "deleted", "Đã xóa mức chiết khấu chưa áp dụng.");

    private final CommissionService commissionService;

    @GetMapping
    public String index(@RequestParam(required = false) String keyword,
                        @RequestParam(defaultValue = "1") int page,
                        @RequestParam(required = false) String msg,
                        Model model) {
        addMessage(msg, model);
        return showIndex(keyword, page, new CommissionForm(), model);
    }

    @PostMapping("/default")
    public String addDefault(@Valid @ModelAttribute("form") CommissionForm form, BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                commissionService.addRate(null, form);
                return "redirect:/admin/commissions?msg=added";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showIndex(null, 1, form, model);
    }

    @GetMapping("/shops/{shopId}")
    public String shop(@PathVariable Long shopId, @RequestParam(required = false) String msg, Model model) {
        addMessage(msg, model);
        return showShop(shopId, new CommissionForm(), model);
    }

    @PostMapping("/shops/{shopId}")
    public String addShopRate(@PathVariable Long shopId, @Valid @ModelAttribute("form") CommissionForm form,
                              BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                commissionService.addRate(shopId, form);
                return "redirect:/admin/commissions/shops/" + shopId + "?msg=added";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showShop(shopId, form, model);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(required = false) Long shopId, Model model) {
        try {
            Long owner = commissionService.deleteRate(id);
            return owner == null
                    ? "redirect:/admin/commissions?msg=deleted"
                    : "redirect:/admin/commissions/shops/" + owner + "?msg=deleted";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return shopId == null ? showIndex(null, 1, new CommissionForm(), model) : showShop(shopId, new CommissionForm(), model);
        }
    }

    private String showIndex(String keyword, int page, CommissionForm form, Model model) {
        model.addAttribute("defaultRate", commissionService.currentDefaultRate());
        model.addAttribute("history", commissionService.defaultHistory());
        model.addAttribute("page", commissionService.shopRows(keyword, page - 1));
        model.addAttribute("keyword", keyword);
        model.addAttribute("form", form);
        return INDEX_VIEW;
    }

    private String showShop(Long shopId, CommissionForm form, Model model) {
        model.addAttribute("shop", commissionService.shopRow(shopId));
        model.addAttribute("defaultRate", commissionService.currentDefaultRate());
        model.addAttribute("history", commissionService.shopHistory(shopId));
        model.addAttribute("form", form);
        return SHOP_VIEW;
    }

    private static void addMessage(String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
    }
}
