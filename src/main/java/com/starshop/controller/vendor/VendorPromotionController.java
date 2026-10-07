package com.starshop.controller.vendor;

import com.starshop.dto.promotion.PromotionForm;
import com.starshop.dto.promotion.PromotionStatus;
import com.starshop.entity.enums.PromotionType;
import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.PromotionService;
import com.starshop.util.EnumParams;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Vendor quản lý khuyến mãi / mã giảm giá của shop mình (phạm vi luôn là sản phẩm của shop).
 * Dùng chung kiểm tra và quy tắc khóa với khuyến mãi của Admin (B4); quyền sở hữu kiểm tra ở PromotionService.
 */
@Controller
@RequestMapping("/vendor/promotions")
@RequiredArgsConstructor
public class VendorPromotionController {

    private static final String LIST_VIEW = "vendor/promotions/list";
    private static final String FORM_VIEW = "vendor/promotions/form";
    private static final Map<String, String> MESSAGES = Map.of(
            "created", "Đã tạo chương trình khuyến mãi.",
            "updated", "Đã cập nhật chương trình.",
            "toggled", "Đã đổi trạng thái chương trình.",
            "deleted", "Đã xóa chương trình.");

    private final PromotionService promotionService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String msg,
                       Model model) {
        PromotionStatus statusFilter = EnumParams.parse(PromotionStatus.class, status);
        model.addAttribute("page", promotionService.searchForShop(user.getId(), keyword, statusFilter, page - 1));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("statuses", PromotionStatus.values());
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        PromotionForm form = new PromotionForm();
        // Gợi ý: bắt đầu từ giờ tới, kéo dài 7 ngày
        LocalDateTime start = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        form.setStartAt(start);
        form.setEndAt(start.plusDays(7));
        form.setType(PromotionType.PRODUCT_PERCENT);
        return showForm(form, null, false, model);
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserPrincipal user,
                         @Valid @ModelAttribute("form") PromotionForm form, BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                promotionService.createForShop(user.getId(), form);
                return "redirect:/vendor/promotions?msg=created";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showForm(form, null, false, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        return showForm(promotionService.getShopForm(user.getId(), id), id,
                promotionService.isShopLocked(user.getId(), id), model);
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @Valid @ModelAttribute("form") PromotionForm form, BindingResult result, Model model) {
        boolean locked = promotionService.isShopLocked(user.getId(), id);
        if (!result.hasErrors()) {
            try {
                promotionService.updateForShop(user.getId(), id, form);
                return "redirect:/vendor/promotions?msg=updated";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showForm(form, id, locked, model);
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        promotionService.toggleForShop(user.getId(), id);
        return "redirect:/vendor/promotions?msg=toggled";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        try {
            promotionService.deleteForShop(user.getId(), id);
            return "redirect:/vendor/promotions?msg=deleted";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return list(user, null, null, 1, null, model);
        }
    }

    private String showForm(PromotionForm form, Long id, boolean locked, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("promotionId", id);
        model.addAttribute("locked", locked);
        model.addAttribute("types", PromotionType.values());
        return FORM_VIEW;
    }
}
