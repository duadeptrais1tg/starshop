package com.starshop.controller.web;

import com.starshop.dto.checkout.CheckoutRequest;
import com.starshop.entity.enums.PaymentMethod;
import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.CheckoutService;
import com.starshop.util.EnumParams;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Trang checkout (/checkout/** bắt buộc đăng nhập).
 * GET: xem trước – mỗi lần đổi địa chỉ / nhà vận chuyển / mã giảm giá, trang tải lại và server tính lại toàn bộ.
 * POST: đặt hàng. Tham số: items=1,2,3 (id dòng giỏ), addressId, carrierId, payment, coupon_{shopId}, note_{shopId}.
 */
@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private static final String VIEW = "web/checkout/checkout";
    private static final String COUPON_PREFIX = "coupon_";
    private static final String NOTE_PREFIX = "note_";

    private final CheckoutService checkoutService;

    @GetMapping
    public String checkout(@AuthenticationPrincipal UserPrincipal user,
                           @RequestParam Map<String, String> params, Model model) {
        return show(user.getId(), toRequest(params), model);
    }

    @PostMapping
    public String placeOrder(@AuthenticationPrincipal UserPrincipal user,
                             @RequestParam Map<String, String> params, Model model) {
        CheckoutRequest request = toRequest(params);
        try {
            String txnRef = checkoutService.placeOrder(user.getId(), request);
            return "redirect:/checkout/success?ref=" + txnRef;
        } catch (BusinessException e) {
            model.addAttribute("placeError", e.getMessage());
            return show(user.getId(), request, model);
        }
    }

    @GetMapping("/success")
    public String success(@AuthenticationPrincipal UserPrincipal user, @RequestParam String ref, Model model) {
        model.addAttribute("result", checkoutService.success(user.getId(), ref));
        return "web/checkout/success";
    }

    private String show(Long userId, CheckoutRequest request, Model model) {
        try {
            model.addAttribute("checkout", checkoutService.preview(userId, request));
            return VIEW;
        } catch (BusinessException e) {
            // Không còn sản phẩm nào (đã đặt ở tab khác, đã xóa khỏi giỏ...) -> về giỏ hàng
            return "redirect:/cart?msg=noSelection";
        }
    }

    private static CheckoutRequest toRequest(Map<String, String> params) {
        CheckoutRequest request = new CheckoutRequest();
        String items = params.get("items");
        if (items != null) {
            for (String part : items.split(",")) {
                Long id = parseId(part);
                if (id != null) {
                    request.getItemIds().add(id);
                }
            }
        }
        request.setAddressId(parseId(params.get("addressId")));
        request.setCarrierId(parseId(params.get("carrierId")));
        request.setPaymentMethod(EnumParams.parse(PaymentMethod.class, params.get("payment")));
        params.forEach((key, value) -> {
            if (key.startsWith(COUPON_PREFIX)) {
                Long shopId = parseId(key.substring(COUPON_PREFIX.length()));
                if (shopId != null) {
                    request.getCoupons().put(shopId, value);
                }
            } else if (key.startsWith(NOTE_PREFIX)) {
                Long shopId = parseId(key.substring(NOTE_PREFIX.length()));
                if (shopId != null) {
                    request.getNotes().put(shopId, value);
                }
            }
        });
        return request;
    }

    private static Long parseId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long id = Long.parseLong(value.trim());
            return id > 0 ? id : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
