package com.starshop.dto;

import com.starshop.entity.enums.RoleName;
import com.starshop.security.UserPrincipal;
import lombok.Getter;

/**
 * Thông tin người đang đăng nhập để hiển thị trên header (không chứa mật khẩu).
 * Dùng class có getter thay vì record vì EL của Tomcat 10 không đọc được accessor của record.
 */
@Getter
public class CurrentUser {

    private final Long id;
    private final String fullName;
    private final String email;
    private final String avatarUrl;
    private final String homePath;
    private final boolean admin;
    private final boolean manager;
    private final boolean vendor;
    private final boolean shipper;

    public CurrentUser(UserPrincipal principal) {
        this.id = principal.getId();
        this.fullName = principal.getFullName();
        this.email = principal.getEmail();
        this.avatarUrl = principal.getAvatarUrl();
        this.homePath = principal.getHomePath();
        this.admin = principal.hasRole(RoleName.ADMIN);
        this.manager = principal.hasRole(RoleName.MANAGER);
        this.vendor = principal.hasRole(RoleName.VENDOR);
        this.shipper = principal.hasRole(RoleName.SHIPPER);
    }

    /** Chữ cái đầu của tên cuối, ví dụ "Nguyễn Văn An" -> "A" (hiển thị trong avatar tròn). */
    public String getInitial() {
        if (fullName == null || fullName.isBlank()) {
            return "?";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts[parts.length - 1].substring(0, 1).toUpperCase();
    }
}
