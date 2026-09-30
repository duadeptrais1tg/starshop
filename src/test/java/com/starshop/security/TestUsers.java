package com.starshop.security;

import com.starshop.entity.Role;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;

import java.util.HashSet;
import java.util.Set;

/**
 * Tạo User giả cho test (không cần database).
 */
public final class TestUsers {

    private TestUsers() {
    }

    public static User user(Long id, String email, boolean enabled, boolean locked, RoleName... roles) {
        User user = User.builder()
                .fullName("Test")
                .email(email)
                .password("$2a$10$hash")
                .enabled(enabled)
                .locked(locked)
                .build();
        user.setId(id);
        Set<Role> roleSet = new HashSet<>();
        for (RoleName name : roles) {
            roleSet.add(Role.builder().name(name).build());
        }
        user.setRoles(roleSet);
        return user;
    }

    public static UserPrincipal principal(Long id, String email, RoleName... roles) {
        return UserPrincipal.from(user(id, email, true, false, roles));
    }
}
