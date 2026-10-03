package com.starshop.controller.admin;

import com.starshop.dto.promotion.PromotionForm;
import com.starshop.dto.promotion.PromotionStatus;
import com.starshop.entity.enums.PromotionScope;
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
 * Admin quản lý khuyến mãi toàn sàn / theo danh mục.
 */
@Controller
@RequestMapping("/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionController {

    private static final String LIST_VIEW = "admin/promotions/list";
    private static final String FORM_VIEW = "admin/promotions/form";
    private static final Map<String, String> MESSAGES = Map.of(
            "created", "Đã tạo chương trình khuyến mãi.",
            "updated", "Đã cập nhật chương trình.",
            "toggled", "Đã đổi trạng thái chương trình.",
            "deleted", "Đã xóa chương trình.");

    private final PromotionService promotionService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String msg,
                       Model model) {
        PromotionStatus statusFilter = EnumParams.parse(PromotionStatus.class, status);
        model.addAttribute("page", promotionService.search(keyword, statusFilter, page - 1));
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
    public String create(@Valid @ModelAttribute("form") PromotionForm form, BindingResult result,
                         @AuthenticationPrincipal UserPrincipal admin, Model model) {
        if (!result.hasErrors()) {
            try {
                promotionService.create(form, admin.getId());
                return "redirect:/admin/promotions?msg=created";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showForm(form, null, false, model);
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        return showForm(promotionService.getForm(id), id, promotionService.isLocked(id), model);
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") PromotionForm form,
                         BindingResult result, Model model) {
        boolean locked = promotionService.isLocked(id);
        if (!result.hasErrors()) {
            try {
                promotionService.update(id, form);
                return "redirect:/admin/promotions?msg=updated";
            } catch (BusinessException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return showForm(form, id, locked, model);
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        promotionService.toggleActive(id);
        return "redirect:/admin/promotions?msg=toggled";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Model model) {
        try {
            promotionService.delete(id);
            return "redirect:/admin/promotions?msg=deleted";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return list(null, null, 1, null, model);
        }
    }

    private String showForm(PromotionForm form, Long id, boolean locked, Model model) {
        model.addAttribute("form", form);
        model.addAttribute("promotionId", id);
        model.addAttribute("locked", locked);
        model.addAttribute("types", PromotionType.values());
        model.addAttribute("scopes", new PromotionScope[]{PromotionScope.PLATFORM, PromotionScope.CATEGORY});
        model.addAttribute("categories", promotionService.categoryOptions());
        return FORM_VIEW;
    }
}
