package com.starshop.dto.admin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Bộ lọc trạng thái tài khoản ở trang quản lý user.
 */
@Getter
@RequiredArgsConstructor
public enum UserStatusFilter {
    ACTIVE("Đang hoạt động"),
    LOCKED("Bị khóa"),
    UNVERIFIED("Chưa kích hoạt");

    private final String label;
}
