package com.starshop.security;

import com.starshop.entity.enums.RoleName;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;
import java.util.List;

import static com.starshop.security.TestUsers.principal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofHours(1), false));

    @Test
    void generateAndParse_containsEmailIdAndRoles() {
        String token = jwtService.generateToken(principal(7L, "vendor@starshop.vn", RoleName.USER, RoleName.VENDOR));

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo("vendor@starshop.vn");
        assertThat(claims.get("uid", Long.class)).isEqualTo(7L);
        assertThat(claims.get("roles", List.class)).containsExactlyInAnyOrder("USER", "VENDOR");
    }

    @Test
    void rejectsExpiredToken() {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-1), false));
        String token = shortLived.generateToken(principal(1L, "a@starshop.vn", RoleName.USER));

        assertThatThrownBy(() -> jwtService.parseToken(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTokenSignedWithAnotherSecret() {
        JwtService attacker = new JwtService(new JwtProperties("another-secret-another-secret-another-1", Duration.ofHours(1), false));
        String forged = attacker.generateToken(principal(1L, "admin@starshop.vn", RoleName.ADMIN));

        assertThatThrownBy(() -> jwtService.parseToken(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsTamperedAndGarbageToken() {
        String token = jwtService.generateToken(principal(1L, "a@starshop.vn", RoleName.USER));
        String tampered = token.substring(0, token.length() - 2) + (token.endsWith("A") ? "BB" : "AA");

        assertThatThrownBy(() -> jwtService.parseToken(tampered)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtService.parseToken("abc.def.ghi")).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> jwtService.parseToken("")).isInstanceOf(JwtException.class);
    }

    @Test
    void refusesMissingShortOrSampleSecret() {
        assertThatThrownBy(() -> new JwtService(new JwtProperties(null, Duration.ofHours(1), false)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(new JwtProperties("too-short", Duration.ofHours(1), false)))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(new JwtProperties(
                "change_me_to_a_random_string_at_least_32_chars", Duration.ofHours(1), false)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cookieIsHttpOnlyLaxAndExpiresWithToken() {
        ResponseCookie cookie = jwtService.createAccessTokenCookie("abc");

        assertThat(cookie.getName()).isEqualTo("ACCESS_TOKEN");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getMaxAge()).isEqualTo(Duration.ofHours(1));
        assertThat(jwtService.clearAccessTokenCookie().getMaxAge()).isZero();
    }
}
