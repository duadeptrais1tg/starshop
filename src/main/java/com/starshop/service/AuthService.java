package com.starshop.service;

import com.starshop.security.UserPrincipal;

/**
 * Đăng nhập bằng email + mật khẩu, cấp JWT.
 */
public interface AuthService {

    /**
     * @return người dùng đã xác thực kèm JWT
     * @throws com.starshop.exception.BusinessException sai email/mật khẩu (thông báo chung) hoặc tài khoản bị khóa
     * @throws com.starshop.exception.AccountNotActivatedException đúng mật khẩu nhưng chưa kích hoạt OTP
     */
    LoginResult login(String email, String password);

    record LoginResult(UserPrincipal user, String token) {
    }
}
