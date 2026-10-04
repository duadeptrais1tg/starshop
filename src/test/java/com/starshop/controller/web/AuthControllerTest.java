package com.starshop.controller.web;

import com.starshop.config.SecurityConfig;
import com.starshop.entity.enums.RoleName;
import com.starshop.exception.AccountNotActivatedException;
import com.starshop.exception.BusinessException;
import com.starshop.security.CustomUserDetailsService;
import com.starshop.security.ForbiddenAccessDeniedHandler;
import com.starshop.security.JwtAuthFilter;
import com.starshop.security.JwtService;
import com.starshop.security.LoginRedirectEntryPoint;
import com.starshop.security.UserPrincipal;
import com.starshop.service.CartService;
import com.starshop.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static com.starshop.security.TestUsers.principal;
import static com.starshop.security.TestUsers.user;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, JwtAuthFilter.class, JwtService.class,
        LoginRedirectEntryPoint.class, ForbiddenAccessDeniedHandler.class})
@TestPropertySource(properties = {"jwt.secret=test-secret-test-secret-test-secret-123456", "jwt.expiration=1h"})
class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    /** CurrentUserAdvice cần để đếm giỏ hàng trên header. */
    @MockitoBean
    private CartService cartService;

    @Test
    void login_success_setsHttpOnlyCookie_andGoesToRoleHome() throws Exception {
        givenLogin(principal(2L, "admin@starshop.vn", RoleName.ADMIN));

        mvc.perform(post("/auth/login").with(csrf()).param("email", "admin@starshop.vn").param("password", "Starshop@123"))
                .andExpect(redirectedUrl("/admin"))
                .andExpect(header().string("Set-Cookie", allOf(
                        containsString("ACCESS_TOKEN=token-123"), containsString("HttpOnly"), containsString("SameSite=Lax"))))
                .andExpect(header().stringValues("Set-Cookie", hasItem(allOf(startsWith("XSRF-TOKEN=;"), containsString("Max-Age=0")))));
    }

    @Test
    void login_success_returnsToRequestedPage() throws Exception {
        givenLogin(principal(1L, "user@starshop.vn", RoleName.USER));

        mvc.perform(post("/auth/login").with(csrf()).param("email", "user@starshop.vn").param("password", "x")
                        .param("redirect", "/user/orders?page=2"))
                .andExpect(redirectedUrl("/user/orders?page=2"));
    }

    @Test
    void login_ignoresExternalRedirect() throws Exception {
        givenLogin(principal(1L, "user@starshop.vn", RoleName.USER));

        mvc.perform(post("/auth/login").with(csrf()).param("email", "user@starshop.vn").param("password", "x")
                        .param("redirect", "//trang-gia.com"))
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void login_badCredentials_showsGenericError_andNoCookie() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new BusinessException("Email hoặc mật khẩu không đúng."));

        mvc.perform(post("/auth/login").with(csrf()).param("email", "ai@gmail.com").param("password", "sai"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("error", "Email hoặc mật khẩu không đúng."))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void login_notActivated_offersOtpLink() throws Exception {
        when(authService.login(anyString(), anyString())).thenThrow(new AccountNotActivatedException("an@gmail.com"));

        mvc.perform(post("/auth/login").with(csrf()).param("email", "an@gmail.com").param("password", "Starshop123"))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("notActivatedEmail", "an@gmail.com"));
    }

    @Test
    void login_invalidForm_staysOnPage() throws Exception {
        mvc.perform(post("/auth/login").with(csrf()).param("email", "khong-phai-email").param("password", ""))
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeHasFieldErrors("form", "email", "password"));
    }

    @Test
    void loginPage_whenAlreadyLoggedIn_redirectsHome() throws Exception {
        mvc.perform(get("/auth/login").cookie(cookieFor("vendor@starshop.vn", RoleName.USER, RoleName.VENDOR)))
                .andExpect(redirectedUrl("/vendor"));
    }

    @Test
    void logout_clearsCookie_onlyWithPostAndCsrf() throws Exception {
        mvc.perform(post("/auth/logout").with(csrf()))
                .andExpect(redirectedUrl("/auth/login?logout"))
                .andExpect(header().string("Set-Cookie", allOf(containsString("ACCESS_TOKEN="), containsString("Max-Age=0"))));

        mvc.perform(post("/auth/logout")).andExpect(status().isForbidden());
        mvc.perform(get("/auth/logout")).andExpect(status().isMethodNotAllowed());
    }

    private void givenLogin(UserPrincipal user) {
        when(authService.login(anyString(), anyString())).thenReturn(new AuthService.LoginResult(user, "token-123"));
    }

    private Cookie cookieFor(String email, RoleName... roles) {
        var u = user(3L, email, true, false, roles);
        when(userDetailsService.loadUserByUsername(email)).thenAnswer(inv -> UserPrincipal.from(u));
        return new Cookie("ACCESS_TOKEN", jwtService.generateToken(UserPrincipal.from(u)));
    }
}
