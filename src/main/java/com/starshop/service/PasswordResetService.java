package com.starshop.service;

import com.starshop.dto.ResetPasswordRequest;

/**
 * Quên mật khẩu: gửi OTP loại RESET_PASSWORD qua email rồi đặt mật khẩu mới.
 */
public interface PasswordResetService {

    /**
     * Gửi mã OTP nếu email thuộc một tài khoản đã kích hoạt.
     * Không báo lỗi khi email không tồn tại / vừa gửi chưa đủ 60 giây, để không ai dò được email nào đã đăng ký.
     */
    void requestReset(String email);

    /**
     * @throws com.starshop.exception.BusinessException mã OTP sai / hết hạn / mật khẩu không hợp lệ
     */
    void resetPassword(ResetPasswordRequest request);
}
