package com.starshop.service.impl;

import com.starshop.dto.ResetPasswordRequest;
import com.starshop.entity.User;
import com.starshop.entity.enums.OtpType;
import com.starshop.exception.BusinessException;
import com.starshop.exception.OtpException;
import com.starshop.repository.UserRepository;
import com.starshop.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PasswordResetServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final OtpService otpService = mock(OtpService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final PasswordResetServiceImpl service = new PasswordResetServiceImpl(userRepository, otpService, encoder);

    private User active;

    @BeforeEach
    void setUp() {
        active = User.builder().email("an@gmail.com").fullName("An").password(encoder.encode("CuMatKhau1")).enabled(true).build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(active));
    }

    @Test
    void requestReset_activeAccount_sendsResetOtp() {
        service.requestReset(" AN@gmail.com ");
        verify(otpService).issue(active, OtpType.RESET_PASSWORD);
    }

    @Test
    void requestReset_unknownOrInactiveEmail_sendsNothingAndNoError() {
        service.requestReset("ai-do@gmail.com");
        User inactive = User.builder().email("moi@gmail.com").enabled(false).build();
        when(userRepository.findByEmail("moi@gmail.com")).thenReturn(Optional.of(inactive));
        service.requestReset("moi@gmail.com");

        verify(otpService, never()).issue(any(), any());
    }

    @Test
    void requestReset_withinCooldown_isSilentlyIgnored() {
        when(otpService.secondsUntilResend(active, OtpType.RESET_PASSWORD)).thenReturn(30L);
        service.requestReset("an@gmail.com");
        verify(otpService, never()).issue(any(), any());
    }

    @Test
    void resetPassword_correctOtp_changesPassword() {
        service.resetPassword(form("an@gmail.com", "123456", "MatKhauMoi9", "MatKhauMoi9"));

        verify(otpService).verify(active, OtpType.RESET_PASSWORD, "123456");
        assertThat(encoder.matches("MatKhauMoi9", active.getPassword())).isTrue();
    }

    @Test
    void resetPassword_wrongOtp_keepsOldPassword() {
        doThrow(new OtpException("sai")).when(otpService).verify(active, OtpType.RESET_PASSWORD, "000000");

        assertThatThrownBy(() -> service.resetPassword(form("an@gmail.com", "000000", "MatKhauMoi9", "MatKhauMoi9")))
                .isInstanceOf(OtpException.class);
        assertThat(encoder.matches("CuMatKhau1", active.getPassword())).isTrue();
    }

    @Test
    void resetPassword_unknownEmail_givesGenericOtpError() {
        assertThatThrownBy(() -> service.resetPassword(form("ai-do@gmail.com", "123456", "MatKhauMoi9", "MatKhauMoi9")))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("Mã OTP không hợp lệ");
    }

    @Test
    void resetPassword_rechecksPasswordRules() {
        assertThatThrownBy(() -> service.resetPassword(form("an@gmail.com", "123456", "yeu", "yeu")))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.resetPassword(form("an@gmail.com", "123456", "MatKhauMoi9", "Khac12345")))
                .hasMessageContaining("không khớp");
        verify(otpService, never()).verify(any(), any(), any());
    }

    private static ResetPasswordRequest form(String email, String code, String password, String confirm) {
        ResetPasswordRequest form = new ResetPasswordRequest();
        form.setEmail(email);
        form.setCode(code);
        form.setPassword(password);
        form.setConfirmPassword(confirm);
        return form;
    }
}
