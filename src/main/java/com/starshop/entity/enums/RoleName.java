package com.starshop.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Vai trò người dùng. Guest (chưa đăng nhập) không lưu trong CSDL.
 */
@Getter
@RequiredArgsConstructor
public enum RoleName {
    USER("Khách hàng"),
    VENDOR("Người bán"),
    MANAGER("Quản lý chi nhánh"),
    ADMIN("Quản trị viên"),
    SHIPPER("Người giao hàng");

    private final String label;
}
