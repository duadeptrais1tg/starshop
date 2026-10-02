package com.starshop.dto.admin;

import com.starshop.entity.enums.RoleName;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Thông tin user hiển thị cho Admin (không có mật khẩu).
 */
@Getter
@Builder
public class UserAdminDto {

    private final Long id;
    private final String fullName;
    private final String email;
    private final String phone;
    private final List<RoleName> roles;
    private final boolean enabled;
    private final boolean locked;
    private final Long storeId;
    private final String storeName;
    private final Long carrierId;
    private final String carrierName;
    private final String createdAt;

    public boolean hasRole(RoleName role) {
        return roles.contains(role);
    }

    /** Dùng trong JSP: ${user.roleNames} chứa 'ADMIN'... */
    public List<String> getRoleNames() {
        return roles.stream().map(Enum::name).toList();
    }

    public String getStatusLabel() {
        if (locked) {
            return UserStatusFilter.LOCKED.getLabel();
        }
        return enabled ? UserStatusFilter.ACTIVE.getLabel() : UserStatusFilter.UNVERIFIED.getLabel();
    }
}
