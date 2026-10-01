package com.starshop.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SafeRedirectTest {

    @ParameterizedTest
    @ValueSource(strings = {"/", "/admin", "/user/orders?page=2", "/products/search?keyword=hoa%20h%E1%BB%93ng", "/shop/a:b"})
    void acceptsInternalPaths(String path) {
        assertThat(SafeRedirect.resolve(path, "/home")).isEqualTo(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://trang-gia.com", "//trang-gia.com", "/\\trang-gia.com", "javascript:alert(1)",
            "/\t/trang-gia.com", "/abc\r\nSet-Cookie:x=1", "admin", "/auth/login", "/auth/logout", ""
    })
    void rejectsExternalOrDangerousTargets(String path) {
        assertThat(SafeRedirect.resolve(path, "/home")).isEqualTo("/home");
    }

    @Test
    void nullFallsBack() {
        assertThat(SafeRedirect.resolve(null, "/vendor")).isEqualTo("/vendor");
    }
}
