package com.starshop.controller.web;

import com.starshop.dto.account.ChangePasswordForm;
import com.starshop.dto.account.ProfileForm;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Hồ sơ cá nhân: sửa thông tin, đổi ảnh đại diện, đổi mật khẩu.
 * Chỉ thao tác trên tài khoản đang đăng nhập (userId lấy từ phiên đăng nhập).
 */
@Controller
@RequestMapping("/user/profile")
@RequiredArgsConstructor
public class ProfileController {

    private static final String VIEW = "web/user/profile";
    private static final Map<String, String> MESSAGES = Map.of(
            "info", "Đã cập nhật thông tin cá nhân.",
            "avatar", "Đã đổi ảnh đại diện.",
            "avatarRemoved", "Đã xóa ảnh đại diện.",
            "password", "Đã đổi mật khẩu.");

    private final ProfileService profileService;

    @GetMapping
    public String profile(@AuthenticationPrincipal UserPrincipal user,
                          @RequestParam(required = false) String msg, Model model) {
        if (msg != null && MESSAGES.containsKey(msg)) {
            model.addAttribute("message", MESSAGES.get(msg));
        }
        return show(user.getId(), profileService.getForm(user.getId()), new ChangePasswordForm(), model);
    }

    @PostMapping
    public String updateInfo(@AuthenticationPrincipal UserPrincipal user,
                             @Valid @ModelAttribute("profileForm") ProfileForm form, BindingResult result, Model model) {
        if (!result.hasErrors()) {
            try {
                profileService.updateProfile(user.getId(), form);
                return "redirect:/user/profile?msg=info";
            } catch (BusinessException e) {
                model.addAttribute("infoError", e.getMessage());
            }
        }
        return show(user.getId(), form, new ChangePasswordForm(), model);
    }

    @PostMapping("/avatar")
    public String changeAvatar(@AuthenticationPrincipal UserPrincipal user,
                               @RequestParam(value = "avatar", required = false) MultipartFile avatar, Model model) {
        try {
            profileService.changeAvatar(user.getId(), avatar);
            return "redirect:/user/profile?msg=avatar";
        } catch (InvalidFileException | FileStorageException e) {
            model.addAttribute("avatarError", e.getMessage());
            return show(user.getId(), profileService.getForm(user.getId()), new ChangePasswordForm(), model);
        }
    }

    @PostMapping("/avatar/remove")
    public String removeAvatar(@AuthenticationPrincipal UserPrincipal user) {
        profileService.removeAvatar(user.getId());
        return "redirect:/user/profile?msg=avatarRemoved";
    }

    @PostMapping("/password")
    public String changePassword(@AuthenticationPrincipal UserPrincipal user,
                                 @Valid @ModelAttribute("passwordForm") ChangePasswordForm form, BindingResult result,
                                 Model model) {
        if (!result.hasErrors()) {
            try {
                profileService.changePassword(user.getId(), form);
                return "redirect:/user/profile?msg=password";
            } catch (BusinessException e) {
                model.addAttribute("passwordError", e.getMessage());
            }
        }
        // Không gửi lại mật khẩu đã nhập về trình duyệt
        form.setCurrentPassword(null);
        form.setPassword(null);
        form.setConfirmPassword(null);
        return show(user.getId(), profileService.getForm(user.getId()), form, model);
    }

    private String show(Long userId, ProfileForm profileForm, ChangePasswordForm passwordForm, Model model) {
        model.addAttribute("profile", profileService.getProfile(userId));
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("passwordForm", passwordForm);
        return VIEW;
    }
}
