package com.starshop.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Tạo / kiểm tra JWT và cookie chứa JWT.
 * Token ký bằng HMAC-SHA (jjwt tự chọn HS256/HS384/HS512 theo độ dài secret); subject = email, claim "uid" = id user, "roles" = danh sách role.
 */
@Service
public class JwtService {

    public static final String ACCESS_TOKEN_COOKIE = "ACCESS_TOKEN";

    private static final String ISSUER = "starshop";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final Duration expiration;
    private final boolean cookieSecure;

    public JwtService(JwtProperties properties) {
        String secret = properties.secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("Thiếu cấu hình jwt.secret (biến môi trường JWT_SECRET) "
                    + "hoặc secret ngắn hơn " + MIN_SECRET_BYTES + " ký tự.");
        }
        if (secret.startsWith("change_me")) {
            throw new IllegalStateException("jwt.secret vẫn là giá trị mẫu, hãy đổi thành chuỗi ngẫu nhiên.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = properties.expiration() != null ? properties.expiration() : Duration.ofHours(8);
        this.cookieSecure = properties.cookieSecure();
    }

    public String generateToken(UserPrincipal user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("roles", user.getRoleNames())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(signingKey)
                .compact();
    }

    /**
     * Kiểm tra chữ ký, hạn dùng, issuer và trả về claims.
     *
     * @throws JwtException token sai chữ ký, hết hạn hoặc sai định dạng
     */
    public Claims parseToken(String token) {
        if (token == null || token.isBlank()) {
            throw new JwtException("Token rỗng");
        }
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Cookie chứa JWT: HttpOnly (JavaScript không đọc được), SameSite=Lax, sống bằng thời hạn token. */
    public ResponseCookie createAccessTokenCookie(String token) {
        return baseCookie(token).maxAge(expiration).build();
    }

    /** Cookie rỗng, hết hạn ngay: dùng khi đăng xuất hoặc token không còn hợp lệ. */
    public ResponseCookie clearAccessTokenCookie() {
        return baseCookie("").maxAge(0).build();
    }

    public Duration getExpiration() {
        return expiration;
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, value)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path("/");
    }
}
