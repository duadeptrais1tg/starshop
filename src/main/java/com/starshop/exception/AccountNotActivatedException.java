package com.starshop.exception;

import lombok.Getter;

/**
 * Đăng nhập đúng mật khẩu nhưng tài khoản chưa xác thực OTP.
 */
@Getter
public class AccountNotActivatedException extends BusinessException {

    private final String email;

    public AccountNotActivatedException(String email) {
        super("Tài khoản chưa được kích hoạt. Vui lòng nhập mã OTP đã gửi tới email của bạn.");
        this.email = email;
    }
}
