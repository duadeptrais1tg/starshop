package com.starshop.exception;

/**
 * Lỗi khi xác thực / gửi OTP (sai mã, hết hạn, quá số lần, gửi lại quá sớm...).
 */
public class OtpException extends BusinessException {

    public OtpException(String message) {
        super(message);
    }
}
