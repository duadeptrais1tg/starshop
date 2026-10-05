package com.starshop.service.impl;

import com.starshop.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Mỗi phút: hủy các giao dịch VNPAY khách bỏ dở (quá hạn thanh toán) để trả lại tồn kho và lượt mã.
 * Mỗi giao dịch xử lý trong transaction riêng, lỗi giao dịch này không ảnh hưởng giao dịch khác.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentExpiryJob {

    private final PaymentService paymentService;

    @Scheduled(fixedDelayString = "PT1M", initialDelayString = "PT1M")
    public void cancelExpiredPayments() {
        for (String txnRef : paymentService.staleVnpayTxnRefs()) {
            try {
                paymentService.expire(txnRef);
                log.info("Đã hủy giao dịch VNPAY quá hạn {}", txnRef);
            } catch (RuntimeException e) {
                log.error("Không hủy được giao dịch VNPAY quá hạn {}", txnRef, e);
            }
        }
    }
}
