package com.starshop.controller.api;

import com.starshop.service.PaymentService;
import com.starshop.service.PaymentService.VnpayCallbackResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * IPN: VNPAY gọi server-to-server báo kết quả giao dịch. Không có cookie / CSRF (đã mở trong SecurityConfig
 * cho /api/payment/**), xác thực bằng chữ ký HMAC-SHA512. Trả về JSON theo định dạng VNPAY yêu cầu.
 */
@Slf4j
@RestController
@RequestMapping("/api/payment/vnpay")
@RequiredArgsConstructor
public class PaymentCallbackApiController {

    private final PaymentService paymentService;

    @GetMapping("/ipn")
    public Map<String, String> ipn(@RequestParam Map<String, String> params) {
        try {
            VnpayCallbackResult result = paymentService.handleVnpayCallback(params);
            return Map.of("RspCode", result.rspCode(), "Message", result.message());
        } catch (RuntimeException e) {
            // VNPAY sẽ gọi lại IPN khi nhận mã khác 00/02
            log.error("Lỗi xử lý IPN VNPAY txnRef={}", params.get("vnp_TxnRef"), e);
            return Map.of("RspCode", "99", "Message", "Unknown error");
        }
    }
}
