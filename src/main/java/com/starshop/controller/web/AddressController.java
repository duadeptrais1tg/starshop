package com.starshop.controller.web;

import com.starshop.dto.account.AddressForm;
import com.starshop.exception.BusinessException;
import com.starshop.security.SafeRedirect;
import com.starshop.security.UserPrincipal;
import com.starshop.service.AddressService;
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

import java.util.Map;

/**
 * Sổ địa chỉ nhận hàng. Kiểm tra quyền sở hữu nằm ở AddressService (theo userId đang đăng nhập),
 * nên sửa id trên URL thành địa chỉ người khác chỉ nhận về 404.
 */
@Controller
@RequestMapping("/user/addresses")
@RequiredArgsConstructor
public class AddressController {

    private static final String LIST_VIEW = "web/user/addresses";
    private static final String FORM_VIEW = "web/user/address-form";
    private static final Map<String, String> MESSAGES = Map.of(
            "created", "Đã thêm địa chỉ.",
            "updated", "Đã cập nhật địa chỉ.",
            "deleted", "Đã xóa địa chỉ.",
            "default", "Đã đặt làm địa chỉ mặc định.");

    private final AddressService addressService;

    @GetMapping
    public String list(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        model.addAttribute("addresses", addressService.list(user.getId()));
        model.addAttribute("maxAddresses", AddressService.MAX_ADDRESSES);
        return LIST_VIEW;
    }

    /** redirect (tùy chọn): trang quay lại sau khi thêm, ví dụ từ trang checkout. */
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("form", new AddressForm());
        model.addAttribute("redirect", SafeRedirect.resolve(redirect, null));
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@AuthenticationPrincipal UserPrincipal user,
                         @Valid @ModelAttribute("form") AddressForm form, BindingResult result,
                         @RequestParam(required = false) String redirect, Model model) {
        model.addAttribute("redirect", SafeRedirect.resolve(redirect, null));
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            addressService.create(user.getId(), form);
            return "redirect:" + SafeRedirect.resolve(redirect, "/user/addresses?msg=created");
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id, Model model) {
        model.addAttribute("form", addressService.getForm(user.getId(), id));
        model.addAttribute("addressId", id);
        return FORM_VIEW;
    }

    @PostMapping("/{id}")
    public String update(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id,
                         @Valid @ModelAttribute("form") AddressForm form, BindingResult result, Model model) {
        model.addAttribute("addressId", id);
        if (result.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            addressService.update(user.getId(), id, form);
            return "redirect:/user/addresses?msg=updated";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return FORM_VIEW;
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        addressService.delete(user.getId(), id);
        return "redirect:/user/addresses?msg=deleted";
    }

    @PostMapping("/{id}/default")
    public String setDefault(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long id) {
        addressService.setDefault(user.getId(), id);
        return "redirect:/user/addresses?msg=default";
    }
}
