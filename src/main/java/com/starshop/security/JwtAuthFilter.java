package com.starshop.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * Mỗi request: đọc JWT từ cookie ACCESS_TOKEN, kiểm tra chữ ký/hạn dùng, nạp lại user từ DB
 * rồi đặt vào SecurityContext.
 * <p>
 * Nạp lại từ DB (thay vì chỉ tin role trong token) để tài khoản vừa bị khóa, bị đổi role
 * hoặc bị xóa mất quyền ngay ở request kế tiếp, không phải đợi token hết hạn.
 * Token lỗi/hết hạn thì xóa cookie và coi như khách (Guest).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String[] STATIC_PREFIXES = {"/css/", "/js/", "/img/", "/vendor-template/", "/favicon"};

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return Arrays.stream(STATIC_PREFIXES).anyMatch(path::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String token = readTokenFromCookie(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticate(token, request, response);
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request, HttpServletResponse response) {
        try {
            String email = jwtService.parseToken(token).getSubject();
            UserPrincipal user = userDetailsService.loadUserByUsername(email);
            if (!user.isEnabled() || !user.isAccountNonLocked()) {
                clearCookie(response);
                return;
            }
            user.eraseCredentials();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | IllegalArgumentException | UsernameNotFoundException e) {
            log.debug("JWT không hợp lệ: {}", e.getMessage());
            clearCookie(response);
        }
    }

    private void clearCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, jwtService.clearAccessTokenCookie().toString());
    }

    private static String readTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> JwtService.ACCESS_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst()
                .orElse(null);
    }
}
