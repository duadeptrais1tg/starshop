package com.starshop.controller.web;

import com.starshop.exception.BusinessException;
import com.starshop.security.UserPrincipal;
import com.starshop.service.PaymentService;
import com.starshop.service.PaymentService.VnpayCallbackResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Trình duyệt của khách quay về từ VNPAY (return URL) và thanh toán tiếp giao dịch còn chờ.
 * <p>
 * Return URL cũng xử lý kết quả (giống IPN, chỉ xử lý 1 lần): khi chạy localhost, VNPAY không gọi được IPN.
 * Mọi thứ đều kiểm tra lại chữ ký và số tiền, không tin tham số trên URL.
 */
@Controller
@RequestMapping("/payment/vnpay")
@RequiredArgsConstructor
public class PaymentController {

    private static final String ERROR_VIEW = "web/checkout/payment-error";

    private final PaymentService paymentService;

    @GetMapping("/return")
    public String vnpayReturn(@RequestParam Map<String, String> params, Model model) {
        VnpayCallbackResult result = paymentService.handleVnpayCallback(params);
        if (result.txnRef() == null || "04".equals(result.rspCode())) {
            model.addAttribute("error", "Kết quả thanh toán không hợp lệ (sai chữ ký hoặc số tiền). "
                    + "Nếu bạn đã bị trừ tiền, vui lòng liên hệ StarShop kèm mã giao dịch VNPAY.");
            return ERROR_VIEW;
        }
        return "redirect:/checkout/success?ref=" + result.txnRef();
    }

    /** Khách đóng trang VNPAY rồi quay lại: thanh toán tiếp (trong thời hạn). */
    @GetMapping("/pay")
    public String pay(@AuthenticationPrincipal UserPrincipal user, @RequestParam String ref,
                      HttpServletRequest request, Model model) {
        try {
            return "redirect:" + paymentService.createVnpayUrl(user.getId(), ref, request.getRemoteAddr());
        } catch (BusinessException e) {
            model.addAttribute("error", e.getMessage());
            return ERROR_VIEW;
        }
    }
}
