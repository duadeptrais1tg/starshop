package com.starshop.service.event;

import com.starshop.entity.enums.OtpType;

/**
 * Phát ra khi vừa tạo OTP mới. MailService nghe sự kiện này (sau khi transaction commit) để gửi email.
 *
 * @param email          email nhận
 * @param fullName       tên hiển thị trong email
 * @param code           mã OTP 6 số
 * @param type           đăng ký / quên mật khẩu
 * @param validMinutes   số phút mã còn hiệu lực
 */
public record OtpIssuedEvent(String email, String fullName, String code, OtpType type, long validMinutes) {
}
