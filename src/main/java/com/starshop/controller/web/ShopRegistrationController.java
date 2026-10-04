package com.starshop.controller.web;

import com.starshop.dto.shop.MyShopDto;
import com.starshop.dto.shop.ShopForm;
import com.starshop.exception.BusinessException;
import com.starshop.exception.FileStorageException;
import com.starshop.exception.InvalidFileException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.ShopService;
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

/**
 * "Mở shop": user gửi yêu cầu, xem trạng thái chờ duyệt / bị từ chối (sửa và gửi lại).
 * Được duyệt thì user có thêm role VENDOR và quản lý shop ở /vendor/shop.
 */
@Controller
@RequestMapping("/user/shop")
@RequiredArgsConstructor
public class ShopRegistrationController {

    private static final String VIEW = "web/user/shop";

    private final ShopService shopService;

    @GetMapping
    public String page(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String msg, Model model) {
        if ("submitted".equals(msg)) {
            model.addAttribute("message", "Đã gửi yêu cầu mở shop. Quản trị viên sẽ xem xét và phản hồi sớm.");
        }
        return show(user.getId(), shopService.getForm(user.getId()), model);
    }

    @PostMapping
    public String register(@AuthenticationPrincipal UserPrincipal user,
                           @Valid @ModelAttribute("shopForm") ShopForm form, BindingResult result,
                           @RequestParam(value = "logo", required = false) MultipartFile logo,
                           @RequestParam(value = "banner", required = false) MultipartFile banner,
                           Model model) {
        if (!result.hasErrors()) {
            try {
                shopService.register(user.getId(), form, logo, banner);
                return "redirect:/user/shop?msg=submitted";
            } catch (BusinessException | InvalidFileException | FileStorageException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return show(user.getId(), form, model);
    }

    private String show(Long userId, ShopForm form, Model model) {
        MyShopDto shop = shopService.findMyShop(userId);
        model.addAttribute("shop", shop);
        model.addAttribute("shopForm", form);
        // Chưa có shop hoặc bị từ chối thì mới hiện form đăng ký
        model.addAttribute("canRegister", shop == null || shop.isRejected());
        return VIEW;
    }
}
