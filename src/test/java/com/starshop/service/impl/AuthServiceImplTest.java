package com.starshop.service.impl;

import com.starshop.entity.enums.RoleName;
import com.starshop.exception.AccountNotActivatedException;
import com.starshop.exception.BusinessException;
import com.starshop.security.JwtService;
import com.starshop.security.UserPrincipal;
import com.starshop.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static com.starshop.security.TestUsers.principal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceImplTest {

    private final AuthenticationManager manager = mock(AuthenticationManager.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AuthServiceImpl service = new AuthServiceImpl(manager, jwtService);

    @Test
    void success_returnsUserAndToken_andNormalizesEmail() {
        UserPrincipal admin = principal(1L, "admin@starshop.vn", RoleName.ADMIN);
        when(manager.authenticate(argThat(a -> "admin@starshop.vn".equals(a.getPrincipal()))))
                .thenReturn(UsernamePasswordAuthenticationToken.authenticated(admin, null, admin.getAuthorities()));
        when(jwtService.generateToken(admin)).thenReturn("jwt-token");

        AuthService.LoginResult result = service.login("  Admin@StarShop.vn ", "Starshop@123");

        assertThat(result.token()).isEqualTo("jwt-token");
        assertThat(result.user().getHomePath()).isEqualTo("/admin");
    }

    @Test
    void wrongPasswordOrUnknownEmail_giveSameMessage() {
        when(manager.authenticate(any())).thenThrow(new BadCredentialsException("x"));

        assertThatThrownBy(() -> service.login("ai@gmail.com", "sai"))
                .isInstanceOf(BusinessException.class)
                .hasMessage(AuthServiceImpl.BAD_CREDENTIALS);
    }

    @Test
    void notActivated_suggestsOtpWithEmail() {
        when(manager.authenticate(any())).thenThrow(new DisabledException("x"));

        assertThatThrownBy(() -> service.login("An@gmail.com", "Starshop123"))
                .isInstanceOf(AccountNotActivatedException.class)
                .extracting(e -> ((AccountNotActivatedException) e).getEmail())
                .isEqualTo("an@gmail.com");
    }

    @Test
    void locked_isReported() {
        when(manager.authenticate(any())).thenThrow(new LockedException("x"));

        assertThatThrownBy(() -> service.login("an@gmail.com", "Starshop123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bị khóa");
    }

    @Test
    void homePath_followsHighestRole() {
        assertThat(principal(1L, "a@x.vn", RoleName.USER).getHomePath()).isEqualTo("/");
        assertThat(principal(1L, "a@x.vn", RoleName.USER, RoleName.VENDOR).getHomePath()).isEqualTo("/vendor");
        assertThat(principal(1L, "a@x.vn", RoleName.SHIPPER).getHomePath()).isEqualTo("/shipper");
        assertThat(principal(1L, "a@x.vn", RoleName.MANAGER).getHomePath()).isEqualTo("/manager");
        assertThat(principal(1L, "a@x.vn", RoleName.ADMIN, RoleName.USER).getHomePath()).isEqualTo("/admin");
    }
}
