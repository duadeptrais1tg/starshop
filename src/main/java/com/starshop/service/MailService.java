package com.starshop.service;

import com.starshop.service.event.OtpIssuedEvent;

/**
 * Gửi email của hệ thống.
 */
public interface MailService {

    /** Gửi email chứa mã OTP (đăng ký / quên mật khẩu). */
    void sendOtpEmail(OtpIssuedEvent event);
}
