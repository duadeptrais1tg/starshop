package com.starshop.service;

import com.starshop.entity.User;
import com.starshop.entity.enums.OtpType;

/**
 * Quản lý mã OTP 6 số (đăng ký, quên mật khẩu).
 * Quy tắc: hết hạn sau 5 phút, sai tối đa 5 lần, chỉ gửi lại sau 60 giây, mỗi mã dùng 1 lần,
 * tạo mã mới thì các mã cũ hết hiệu lực.
 */
public interface OtpService {

    /**
     * Tạo OTP mới và gửi email (bất đồng bộ, sau khi transaction commit).
     *
     * @throws com.starshop.exception.OtpException nếu chưa hết 60 giây kể từ lần gửi trước
     */
    void issue(User user, OtpType type);

    /**
     * Kiểm tra mã. Đúng thì đánh dấu đã dùng; sai thì tăng số lần sai.
     *
     * @throws com.starshop.exception.OtpException mã sai / hết hạn / đã dùng / quá số lần
     */
    void verify(User user, OtpType type, String code);

    /** Số giây còn phải đợi trước khi được gửi lại mã (0 = gửi được ngay). */
    long secondsUntilResend(User user, OtpType type);
}
