package com.starshop.controller.admin;

import com.starshop.dto.carrier.CarrierForm;
import com.starshop.exception.BusinessException;
import com.starshop.service.CarrierService;
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
 * Admin quản lý nhà vận chuyển.
 */
@Controller
@RequestMapping("/admin/carriers")
@RequiredArgsConstructor
public class AdminCarrierController {

    private static final String LIST_VIEW = "admin/carriers/list";
    private static final String FORM_VIEW = "admin/carriers/form";
    private static final Map<String, String> MESSAGES = Map.of(
            "created", "Đã thêm nhà vận chuyển.",
            "updated", "Đã cập nhật nhà vận chuyển.",
            "toggled", "Đã đổi trạng thái hoạt động.",
            "deleted", "Đã xóa nhà vận chuyển.");

    private final CarrierService carrierService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String active,
                       @RequestParam(defaultValue = "1") int page,
                       @RequestParam(required = false) String msg,
                       Model model) {
        Boolean activeFilter = "true".equals(active) ? Boolean.TRUE : "false".equals(active) ? Boolean.FALSE : null;
        model.addAttribute("page", carrierService.search(keyword, activeFilter, page - 1));
        model.addAttribute("keyword", keyword);
        model.addAttribute("active", active);
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        return LIST_VIEW;
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", new CarrierForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CarrierForm form, BindingResult result, Model model) {
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            carrierService.create(form);
            return "redirect:/admin/carriers?msg=created";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", carrierService.getForm(id));
        model.addAttribute("carrierId", id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") CarrierForm form,
                         BindingResult result, Model model) {
        model.addAttribute("carrierId", id);
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            carrierService.update(id, form);
            return "redirect:/admin/carriers?msg=updated";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        carrierService.toggleActive(id);
        return "redirect:/admin/carriers?msg=toggled";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Model model) {
        try {
            carrierService.delete(id);
            return "redirect:/admin/carriers?msg=deleted";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return list(null, null, 1, null, model);
        }
    }
}
