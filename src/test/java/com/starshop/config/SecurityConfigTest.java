package com.starshop.config;

import com.starshop.controller.admin.AdminDashboardController;
import com.starshop.controller.vendor.VendorDashboardController;
import com.starshop.controller.web.HomeController;
import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import com.starshop.security.CustomUserDetailsService;
import com.starshop.security.ForbiddenAccessDeniedHandler;
import com.starshop.security.JwtAuthFilter;
import com.starshop.security.JwtService;
import com.starshop.security.LoginRedirectEntryPoint;
import com.starshop.security.UserPrincipal;
import com.starshop.service.AdminDashboardService;
import com.starshop.service.ProductCatalogService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.starshop.security.TestUsers.user;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm tra phân quyền URL, redirect đăng nhập, 403, JWT cookie và CSRF.
 */
@WebMvcTest(controllers = {HomeController.class, AdminDashboardController.class, VendorDashboardController.class})
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class,
        LoginRedirectEntryPoint.class, ForbiddenAccessDeniedHandler.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-test-secret-test-secret-123456",
        "jwt.expiration=1h"
})
class SecurityConfigTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    /** AdminDashboardController cần service này; test chỉ kiểm tra phân quyền nên dùng mock. */
    @MockitoBean
    private AdminDashboardService adminDashboardService;

    @MockitoBean
    private ProductCatalogService productCatalogService;

    private Cookie userCookie;
    private Cookie adminCookie;
    private Cookie vendorCookie;
    private Cookie lockedCookie;

    @BeforeEach
    void setUp() {
        userCookie = login(user(1L, "user@starshop.vn", true, false, RoleName.USER));
        adminCookie = login(user(2L, "admin@starshop.vn", true, false, RoleName.ADMIN));
        vendorCookie = login(user(3L, "vendor@starshop.vn", true, false, RoleName.USER, RoleName.VENDOR));
        lockedCookie = login(user(4L, "locked@starshop.vn", true, true, RoleName.ADMIN));
    }

    // ------------------------------------------------------------------ Guest

    @Test
    void guest_canOpenHomeAndStaticFiles() throws Exception {
        mvc.perform(get("/")).andExpect(status().isOk()).andExpect(forwardedUrl("/WEB-INF/views/web/index.jsp"));
        mvc.perform(get("/css/starshop.css")).andExpect(status().isOk());
    }

    @Test
    void guest_isRedirectedToLoginWithReturnUrl() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?redirect=%2Fadmin"));
        mvc.perform(get("/user/orders?page=2"))
                .andExpect(redirectedUrl("/auth/login?redirect=%2Fuser%2Forders%3Fpage%3D2"));
        mvc.perform(get("/cart")).andExpect(redirectedUrl("/auth/login?redirect=%2Fcart"));
        mvc.perform(get("/checkout")).andExpect(redirectedUrl("/auth/login?redirect=%2Fcheckout"));
    }

    @Test
    void guest_callingApi_gets401Json() throws Exception {
        mvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("\"status\":401")));
    }

    @Test
    void paymentCallback_isPublicAndSkipsCsrf() throws Exception {
        // Không có controller nên 404, quan trọng là KHÔNG bị 401/403
        mvc.perform(post("/api/payment/vnpay/ipn")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- Phân quyền

    @Test
    void adminArea_onlyForAdmin() throws Exception {
        mvc.perform(get("/admin").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/WEB-INF/views/admin/dashboard.jsp"));
        mvc.perform(get("/admin").cookie(userCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/admin").cookie(vendorCookie)).andExpect(status().isForbidden());
    }

    @Test
    void vendorArea_forVendorOnly() throws Exception {
        mvc.perform(get("/vendor").cookie(vendorCookie)).andExpect(status().isOk());
        mvc.perform(get("/vendor").cookie(userCookie)).andExpect(status().isForbidden());
        mvc.perform(get("/vendor").cookie(adminCookie)).andExpect(status().isForbidden());
    }

    @Test
    void loggedInUser_callingForbiddenApi_gets403Json() throws Exception {
        mvc.perform(get("/admin").cookie(userCookie).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isForbidden())
                .andExpect(content().string(containsString("\"status\":403")));
    }

    // ------------------------------------------------------------------- JWT

    @Test
    void lockedAccount_isTreatedAsGuestAndCookieCleared() throws Exception {
        mvc.perform(get("/admin").cookie(lockedCookie))
                .andExpect(redirectedUrl("/auth/login?redirect=%2Fadmin"))
                .andExpect(header().string("Set-Cookie", allOf(containsString("ACCESS_TOKEN="), containsString("Max-Age=0"))));
    }

    @Test
    void forgedOrGarbageToken_isTreatedAsGuest() throws Exception {
        mvc.perform(get("/admin").cookie(new Cookie("ACCESS_TOKEN", "abc.def.ghi")))
                .andExpect(redirectedUrl("/auth/login?redirect=%2Fadmin"))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    // ------------------------------------------------------------------ CSRF

    @Test
    void postWithoutCsrfToken_isRejected() throws Exception {
        mvc.perform(post("/admin").cookie(adminCookie)).andExpect(status().isForbidden());
        // Có token CSRF thì qua được lớp bảo mật (405 vì /admin chỉ có GET)
        mvc.perform(post("/admin").cookie(adminCookie).with(csrf())).andExpect(status().isMethodNotAllowed());
    }

    // --------------------------------------------------------------- helpers

    private Cookie login(User user) {
        when(userDetailsService.loadUserByUsername(user.getEmail()))
                .thenAnswer(invocation -> UserPrincipal.from(user));
        return new Cookie("ACCESS_TOKEN", jwtService.generateToken(UserPrincipal.from(user)));
    }
}
