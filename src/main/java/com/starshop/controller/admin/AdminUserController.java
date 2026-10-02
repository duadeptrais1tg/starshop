package com.starshop.controller.admin;

import com.starshop.dto.admin.RoleAssignmentRequest;
import com.starshop.dto.admin.UserAdminDto;
import com.starshop.dto.admin.UserStatusFilter;
import com.starshop.entity.enums.RoleName;
import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.AdminUserService;
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

import java.util.HashSet;
import java.util.Map;

/**
 * Admin quản lý tài khoản: tìm kiếm, xem chi tiết, khóa/mở khóa, gán vai trò.
 */
@Controller
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private static final Map<String, String> MESSAGES = Map.of(
            "locked", "Đã khóa tài khoản.",
            "unlocked", "Đã mở khóa tài khoản.",
            "roles", "Đã cập nhật vai trò.");

    private final AdminUserService userService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String role,
                       @RequestParam(required = false) String status,
                       @RequestParam(defaultValue = "1") int page,
                       Model model) {
        RoleName roleFilter = EnumParams.parse(RoleName.class, role);
        UserStatusFilter statusFilter = EnumParams.parse(UserStatusFilter.class, status);
        model.addAttribute("page", userService.search(keyword, roleFilter, statusFilter, page - 1));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedRole", roleFilter);
        model.addAttribute("selectedStatus", statusFilter);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("statuses", UserStatusFilter.values());
        return "admin/users/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, @RequestParam(required = false) String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        return showDetail(id, null, model);
    }

    @PostMapping("/{id}/lock")
    public String lock(@PathVariable Long id, @AuthenticationPrincipal UserPrincipal admin, Model model) {
        try {
            userService.lock(id, admin.getId());
            return "redirect:/admin/users/" + id + "?msg=locked";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showDetail(id, null, model);
        }
    }

    @PostMapping("/{id}/unlock")
    public String unlock(@PathVariable Long id) {
        userService.unlock(id);
        return "redirect:/admin/users/" + id + "?msg=unlocked";
    }

    @PostMapping("/{id}/roles")
    public String assignRoles(@PathVariable Long id,
                              @Valid @ModelAttribute("roleForm") RoleAssignmentRequest form, BindingResult result,
                              @AuthenticationPrincipal UserPrincipal admin, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("error", result.getAllErrors().get(0).getDefaultMessage());
            return showDetail(id, form, model);
        }
        try {
            userService.assignRoles(id, form, admin.getId());
            return "redirect:/admin/users/" + id + "?msg=roles";
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return showDetail(id, form, model);
        }
    }

    private String showDetail(Long id, RoleAssignmentRequest submitted, Model model) {
        UserAdminDto user = userService.get(id);
        RoleAssignmentRequest form = submitted;
        if (form == null) {
            form = new RoleAssignmentRequest();
            form.setRoles(new HashSet<>(user.getRoles()));
            form.setStoreId(user.getStoreId());
            form.setCarrierId(user.getCarrierId());
        }
        model.addAttribute("user", user);
        model.addAttribute("roleForm", form);
        model.addAttribute("roles", RoleName.values());
        model.addAttribute("stores", userService.activeStores());
        model.addAttribute("carriers", userService.activeCarriers());
        return "admin/users/detail";
    }
}
