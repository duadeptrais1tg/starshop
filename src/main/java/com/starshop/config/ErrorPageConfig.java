package com.starshop.config;

import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.ModelAndView;

/**
 * Chọn trang lỗi JSP theo mã HTTP (thay cho trang Whitelabel mặc định).
 * 401/403 -> error/403, 404/405 -> error/404, còn lại -> error/500.
 * Trang lỗi được SiteMesh bọc bằng decorator web.
 */
@Configuration
public class ErrorPageConfig {

    @Bean
    public ErrorViewResolver errorViewResolver() {
        return (request, status, model) -> {
            String view = switch (status) {
                case UNAUTHORIZED, FORBIDDEN -> "error/403";
                case NOT_FOUND, METHOD_NOT_ALLOWED -> "error/404";
                default -> "error/500";
            };
            return new ModelAndView(view, model, status);
        };
    }
}
