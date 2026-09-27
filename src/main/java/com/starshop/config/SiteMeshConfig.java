package com.starshop.config;

import jakarta.servlet.DispatcherType;
import org.sitemesh.builder.SiteMeshFilterBuilder;
import org.sitemesh.config.ConfigurableSiteMeshFilter;
import org.sitemesh.config.PathBasedDecoratorSelector;
import org.sitemesh.content.Content;
import org.sitemesh.webapp.WebAppContext;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import java.io.IOException;

/**
 * Cấu hình SiteMesh 3: chọn decorator (layout) theo URL.
 * <pre>
 * /admin/**   -> admin.jsp      /vendor/**  -> vendor.jsp
 * /manager/** -> manager.jsp    /shipper/** -> shipper.jsp
 * /auth/**    -> auth.jsp       còn lại     -> web.jsp (Guest/User)
 * Trang lỗi (403/404/500) luôn dùng web.jsp.
 * </pre>
 */
@Configuration
public class SiteMeshConfig {

    private static final String DECORATOR_DIR = "/WEB-INF/views/decorators/";
    private static final String WEB_DECORATOR = "web.jsp";

    @Bean
    public FilterRegistrationBean<ConfigurableSiteMeshFilter> siteMeshFilter() {
        FilterRegistrationBean<ConfigurableSiteMeshFilter> registration =
                new FilterRegistrationBean<>(new StarShopSiteMeshFilter());
        registration.addUrlPatterns("/*");
        // ERROR: để trang lỗi (403/404/500) cũng được bọc layout
        registration.setDispatcherTypes(DispatcherType.REQUEST, DispatcherType.ERROR);
        // Chạy sau các filter bảo mật: request bị chặn sẽ không tốn công dựng layout
        registration.setOrder(Ordered.LOWEST_PRECEDENCE - 10);
        return registration;
    }

    static class StarShopSiteMeshFilter extends ConfigurableSiteMeshFilter {

        @Override
        protected void applyCustomConfiguration(SiteMeshFilterBuilder builder) {
            AreaDecoratorSelector selector = new AreaDecoratorSelector();
            // SiteMesh 3.2 mặc định tìm decorator trong /WEB-INF/decorators/, đổi sang thư mục views
            selector.setPrefix(DECORATOR_DIR);
            selector.put("/*", WEB_DECORATOR);
            for (String area : new String[]{"admin", "vendor", "manager", "shipper", "auth"}) {
                selector.put("/" + area, area + ".jsp");
                selector.put("/" + area + "/*", area + ".jsp");
            }
            builder.setCustomDecoratorSelector(selector);

            // Không bọc layout cho REST/AJAX, WebSocket và file tĩnh
            builder.addExcludedPath("/api/*")
                    .addExcludedPath("/ws/*")
                    .addExcludedPath("/css/*")
                    .addExcludedPath("/js/*")
                    .addExcludedPath("/img/*")
                    .addExcludedPath("/vendor-template/*");

            builder.setIncludeErrorPages(true);
        }
    }

    /**
     * Chọn decorator theo URL, riêng trang lỗi luôn dùng web.jsp
     * (tránh lộ menu quản trị khi người không có quyền gặp lỗi 403 ở /admin/...).
     */
    static class AreaDecoratorSelector extends PathBasedDecoratorSelector<WebAppContext> {

        @Override
        public String[] selectDecoratorPaths(Content content, WebAppContext context) throws IOException {
            if (context.getRequest().getDispatcherType() == DispatcherType.ERROR) {
                return convertPaths(new String[]{WEB_DECORATOR});
            }
            return super.selectDecoratorPaths(content, context);
        }
    }
}
