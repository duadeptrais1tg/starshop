package com.starshop.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Đã đăng nhập nhưng không đủ quyền (hoặc thiếu/sai CSRF token):
 * trang thường -> trang lỗi 403 (error/403.jsp); AJAX/REST -> JSON 403.
 */
@Component
public class ForbiddenAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        if (SecurityErrorResponder.isApiRequest(request)) {
            SecurityErrorResponder.writeJson(response, HttpServletResponse.SC_FORBIDDEN, "Bạn không có quyền thực hiện thao tác này");
            return;
        }
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
