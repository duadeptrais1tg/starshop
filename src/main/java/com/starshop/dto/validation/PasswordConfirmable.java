package com.starshop.dto.validation;

/**
 * DTO có ô "mật khẩu" và "nhập lại mật khẩu" (đăng ký, đặt lại mật khẩu, đổi mật khẩu).
 */
public interface PasswordConfirmable {

    String getPassword();

    String getConfirmPassword();
}
