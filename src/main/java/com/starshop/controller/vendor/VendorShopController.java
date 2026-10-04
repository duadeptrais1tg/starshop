package com.starshop.controller.vendor;

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
 * Vendor sửa thông tin trang chủ shop: tên, mô tả, logo, banner, địa chỉ lấy hàng, SĐT.
 */
@Controller
@RequestMapping("/vendor/shop")
@RequiredArgsConstructor
public class VendorShopController {

    private static final String VIEW = "vendor/shop";

    private final ShopService shopService;

    @GetMapping
    public String page(@AuthenticationPrincipal UserPrincipal user,
                       @RequestParam(required = false) String msg, Model model) {
        if ("saved".equals(msg)) {
            model.addAttribute("message", "Đã lưu thông tin shop.");
        }
        return show(user.getId(), shopService.getForm(user.getId()), model);
    }

    @PostMapping
    public String update(@AuthenticationPrincipal UserPrincipal user,
                         @Valid @ModelAttribute("shopForm") ShopForm form, BindingResult result,
                         @RequestParam(value = "logo", required = false) MultipartFile logo,
                         @RequestParam(value = "banner", required = false) MultipartFile banner,
                         Model model) {
        if (!result.hasErrors()) {
            try {
                shopService.update(user.getId(), form, logo, banner);
                return "redirect:/vendor/shop?msg=saved";
            } catch (BusinessException | InvalidFileException | FileStorageException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        return show(user.getId(), form, model);
    }

    private String show(Long userId, ShopForm form, Model model) {
        MyShopDto shop = shopService.findMyShop(userId);
        if (shop == null || !shop.isApproved()) {
            // Có role VENDOR nhưng chưa có shop đã duyệt (hiếm): về trang đăng ký / trạng thái
            return "redirect:/user/shop";
        }
        model.addAttribute("shop", shop);
        model.addAttribute("shopForm", form);
        return VIEW;
    }
}
