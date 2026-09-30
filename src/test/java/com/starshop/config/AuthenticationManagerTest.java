package com.starshop.config;

import com.starshop.entity.User;
import com.starshop.entity.enums.RoleName;
import com.starshop.security.CustomUserDetailsService;
import com.starshop.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static com.starshop.security.TestUsers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Kiểm tra AuthenticationManager dùng cho đăng nhập:
 * trạng thái tài khoản chỉ được tiết lộ khi đã nhập ĐÚNG mật khẩu.
 */
class AuthenticationManagerTest {

    private static final String PASSWORD = "Starshop@123";

    private final CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
    private final SecurityConfig config = new SecurityConfig();
    private final PasswordEncoder encoder = config.passwordEncoder();
    private AuthenticationManager manager;

    @BeforeEach
    void setUp() {
        manager = config.authenticationManager(userDetailsService, encoder);
        doThrow(new UsernameNotFoundException("x")).when(userDetailsService).loadUserByUsername(anyString());
        stub(user(1L, "ok@starshop.vn", true, false, RoleName.USER));
        stub(user(2L, "inactive@starshop.vn", false, false, RoleName.USER));
        stub(user(3L, "locked@starshop.vn", true, true, RoleName.USER));
    }

    @Test
    void correctPassword_logsIn() {
        Authentication result = manager.authenticate(login("ok@starshop.vn", PASSWORD));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(((UserPrincipal) result.getPrincipal()).getPassword()).as("mật khẩu đã bị xóa khỏi bộ nhớ").isNull();
    }

    @Test
    void wrongPasswordOrUnknownEmail_giveSameGenericError() {
        assertThatThrownBy(() -> manager.authenticate(login("ok@starshop.vn", "sai-mat-khau")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> manager.authenticate(login("khong-ton-tai@starshop.vn", PASSWORD)))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void wrongPassword_doesNotRevealLockedOrInactiveAccount() {
        assertThatThrownBy(() -> manager.authenticate(login("locked@starshop.vn", "sai-mat-khau")))
                .isInstanceOf(BadCredentialsException.class);
        assertThatThrownBy(() -> manager.authenticate(login("inactive@starshop.vn", "sai-mat-khau")))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void correctPassword_thenReportsAccountStatus() {
        assertThatThrownBy(() -> manager.authenticate(login("inactive@starshop.vn", PASSWORD)))
                .isInstanceOf(DisabledException.class);
        assertThatThrownBy(() -> manager.authenticate(login("locked@starshop.vn", PASSWORD)))
                .isInstanceOf(LockedException.class);
    }

    private void stub(User user) {
        user.setPassword(encoder.encode(PASSWORD));
        doAnswer(inv -> UserPrincipal.from(user)).when(userDetailsService).loadUserByUsername(user.getEmail());
    }

    private static UsernamePasswordAuthenticationToken login(String email, String password) {
        return UsernamePasswordAuthenticationToken.unauthenticated(email, password);
    }
}
