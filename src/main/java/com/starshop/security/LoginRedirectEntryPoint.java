package com.starshop.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Chưa đăng nhập mà vào trang cần đăng nhập:
 * trang thường -> chuyển tới /auth/login?redirect=<trang đang mở>; AJAX/REST -> JSON 401.
 */
@Component
public class LoginRedirectEntryPoint implements AuthenticationEntryPoint {

    public static final String LOGIN_PATH = "/auth/login";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        if (SecurityErrorResponder.isApiRequest(request)) {
            SecurityErrorResponder.writeJson(response, HttpServletResponse.SC_UNAUTHORIZED, "Vui lòng đăng nhập");
            return;
        }
        String contextPath = request.getContextPath();
        String target = request.getRequestURI().substring(contextPath.length());
        if (request.getQueryString() != null) {
            target += "?" + request.getQueryString();
        }
        response.sendRedirect(contextPath + LOGIN_PATH + "?redirect="
                + URLEncoder.encode(target, StandardCharsets.UTF_8));
    }
}
