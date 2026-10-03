package com.starshop.dto.account;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Thông tin hiển thị ở trang hồ sơ (không có mật khẩu).
 */
@Getter
@Builder
public class ProfileDto {

    private final String fullName;
    private final String email;
    private final String phone;
    private final String avatarUrl;
    /** Nhãn vai trò tiếng Việt, ví dụ "Khách hàng", "Người bán". */
    private final List<String> roleLabels;
    private final String createdAt;
}
