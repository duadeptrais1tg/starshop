package com.starshop.config;

import com.starshop.security.CustomUserDetailsService;
import com.starshop.security.ForbiddenAccessDeniedHandler;
import com.starshop.security.JwtAuthFilter;
import com.starshop.security.JwtProperties;
import com.starshop.security.LoginRedirectEntryPoint;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Cấu hình bảo mật: stateless (không dùng session), xác thực bằng JWT trong cookie HttpOnly.
 * <p>
 * CSRF vẫn BẬT: JWT nằm trong cookie nên trình duyệt tự gửi kèm mọi request, kể cả request
 * do trang web khác dựng lên -> vẫn có nguy cơ CSRF. Token CSRF lưu trong cookie XSRF-TOKEN
 * (double-submit, không cần session); form POST gửi kèm hidden input _csrf, AJAX gửi header X-XSRF-TOKEN.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    /** File tĩnh. */
    private static final String[] STATIC_PATHS = {
            "/css/**", "/js/**", "/img/**", "/vendor-template/**", "/favicon.ico"
    };

    /** Trang/API ai cũng xem được (Guest). */
    private static final String[] PUBLIC_PATHS = {
            "/", "/products/**", "/categories/**", "/shop/**", "/auth/**", "/api/products/**", "/error"
    };

    /** Callback từ cổng thanh toán (server-to-server, xác thực bằng chữ ký riêng, không có cookie/CSRF). */
    private static final String[] PAYMENT_CALLBACKS = {"/api/payment/**"};

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthFilter jwtAuthFilter,
                                                   LoginRedirectEntryPoint loginRedirectEntryPoint,
                                                   ForbiddenAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(new CookieCsrfTokenRepository())
                        .ignoringRequestMatchers(PAYMENT_CALLBACKS))
                // Tự viết trang /auth/login, /auth/logout (chức năng đăng nhập) nên tắt form mặc định
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                // Trang cần quay lại sau đăng nhập đã nằm trong tham số ?redirect=
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .authorizeHttpRequests(auth -> auth
                        // Forward tới JSP, include decorator SiteMesh, trang lỗi: đã được kiểm tra ở request gốc
                        .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.INCLUDE, DispatcherType.ERROR).permitAll()
                        .requestMatchers(STATIC_PATHS).permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(PAYMENT_CALLBACKS).permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/manager/**").hasRole("MANAGER")
                        .requestMatchers("/vendor/**").hasRole("VENDOR")
                        .requestMatchers("/shipper/**").hasRole("SHIPPER")
                        .requestMatchers("/user/**", "/cart/**", "/checkout/**").authenticated()
                        // Mặc định: đường dẫn chưa khai báo ở trên đều phải đăng nhập (an toàn hơn là mở)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(loginRedirectEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager dùng khi đăng nhập (xác thực email + mật khẩu).
     * Kiểm tra "chưa kích hoạt / bị khóa" được dời ra SAU bước so mật khẩu, để người không biết
     * mật khẩu không dò được tài khoản nào tồn tại / đang bị khóa.
     */
    @Bean
    public AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                       PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        provider.setPreAuthenticationChecks(user -> { });
        provider.setPostAuthenticationChecks(new AccountStatusUserDetailsChecker());
        return new ProviderManager(provider);
    }

    /** JwtAuthFilter là @Component: tắt đăng ký tự động của Spring Boot để filter chỉ chạy trong chuỗi Security. */
    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilterRegistration(JwtAuthFilter filter) {
        FilterRegistrationBean<JwtAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
