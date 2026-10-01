package com.starshop.service;

import com.starshop.dto.RegisterRequest;

/**
 * Đăng ký tài khoản và kích hoạt bằng OTP gửi qua email.
 */
public interface UserRegistrationService {

    /**
     * Tạo tài khoản chưa kích hoạt (hoặc cập nhật lại nếu email đã đăng ký nhưng chưa kích hoạt)
     * và gửi OTP.
     *
     * @return email đã chuẩn hóa (chữ thường) để chuyển sang trang nhập OTP
     * @throws com.starshop.exception.EmailAlreadyExistsException email đã thuộc tài khoản đang hoạt động
     */
    String register(RegisterRequest request);

    /**
     * Kích hoạt tài khoản nếu OTP đúng.
     *
     * @throws com.starshop.exception.BusinessException mã sai / hết hạn / tài khoản đã kích hoạt
     */
    void activate(String email, String code);

    /** Gửi lại mã OTP kích hoạt (tối thiểu cách lần trước 60 giây). */
    void resendActivationCode(String email);

    /** Số giây còn phải đợi trước khi được gửi lại mã (hiển thị đếm ngược). */
    long secondsUntilResend(String email);
}
