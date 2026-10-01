package com.starshop.service.event;

import com.starshop.service.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Nghe sự kiện OTP mới để gửi email.
 * <ul>
 *   <li>@TransactionalEventListener: chỉ chạy SAU KHI transaction lưu OTP commit thành công
 *       (lưu lỗi thì không gửi mã không dùng được).</li>
 *   <li>@Async: gửi ở thread riêng, request đăng ký trả về ngay không phải chờ máy chủ SMTP.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class OtpEmailListener {

    private final MailService mailService;

    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onOtpIssued(OtpIssuedEvent event) {
        mailService.sendOtpEmail(event);
    }
}
