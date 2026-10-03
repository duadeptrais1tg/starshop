package com.starshop.security;

import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Thông tin người đăng nhập lưu trong SecurityContext (không phải entity, không lộ ra view).
 * Role được đổi thành authority dạng "ROLE_ADMIN" để dùng với hasRole("ADMIN").
 */
@Getter
public class UserPrincipal implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String email;
    private final String fullName;
    private final String avatarUrl;
    private String password;
    private final boolean enabled;
    private final boolean locked;
    private final List<GrantedAuthority> authorities;

    private UserPrincipal(User user) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.fullName = user.getFullName();
        this.avatarUrl = user.getAvatarUrl();
        this.password = user.getPassword();
        this.enabled = user.isEnabled();
        this.locked = user.isLocked();
        this.authorities = user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                .toList();
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user);
    }

    public boolean hasRole(RoleName roleName) {
        String authority = "ROLE_" + roleName.name();
        return authorities.stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }

    /**
     * Trang mặc định sau khi đăng nhập, theo role "cao" nhất:
     * ADMIN -> /admin, MANAGER -> /manager, VENDOR -> /vendor, SHIPPER -> /shipper, còn lại -> /.
     */
    public String getHomePath() {
        if (hasRole(RoleName.ADMIN)) {
            return "/admin";
        }
        if (hasRole(RoleName.MANAGER)) {
            return "/manager";
        }
        if (hasRole(RoleName.VENDOR)) {
            return "/vendor";
        }
        if (hasRole(RoleName.SHIPPER)) {
            return "/shipper";
        }
        return "/";
    }

    public List<String> getRoleNames() {
        return authorities.stream().map(a -> a.getAuthority().substring("ROLE_".length())).toList();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /** Xóa mật khẩu đã mã hóa khỏi bộ nhớ sau khi xác thực xong. */
    @Override
    public void eraseCredentials() {
        this.password = null;
    }
}
