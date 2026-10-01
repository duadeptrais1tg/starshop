package com.starshop.service.impl;

import com.starshop.dto.RegisterRequest;
import com.starshop.entity.Role;
import com.starshop.entity.User;
import com.starshop.entity.enums.OtpType;
import com.starshop.entity.enums.RoleName;
import com.starshop.exception.BusinessException;
import com.starshop.exception.EmailAlreadyExistsException;
import com.starshop.exception.OtpException;
import com.starshop.repository.RoleRepository;
import com.starshop.repository.UserRepository;
import com.starshop.service.OtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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

class UserRegistrationServiceImplTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final RoleRepository roleRepository = mock(RoleRepository.class);
    private final OtpService otpService = mock(OtpService.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRegistrationServiceImpl service =
            new UserRegistrationServiceImpl(userRepository, roleRepository, otpService, encoder);

    @BeforeEach
    void setUp() {
        when(roleRepository.findByName(RoleName.USER)).thenReturn(Optional.of(Role.builder().name(RoleName.USER).build()));
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            if (u.getId() == null) {
                u.setId(99L);
            }
            return u;
        });
    }

    @Test
    void register_createsInactiveUserWithHashedPassword_andSendsOtp() {
        String email = service.register(form("  An@Gmail.COM ", "+84912345678"));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertThat(email).isEqualTo("an@gmail.com");
        assertThat(user.getEmail()).isEqualTo("an@gmail.com");
        assertThat(user.getPhone()).isEqualTo("0912345678");
        assertThat(user.isEnabled()).isFalse();
        assertThat(user.getPassword()).isNotEqualTo("Starshop123");
        assertThat(encoder.matches("Starshop123", user.getPassword())).isTrue();
        assertThat(user.hasRole(RoleName.USER)).isTrue();
        verify(otpService).issue(user, OtpType.REGISTER);
    }

    @Test
    void register_emailOfActiveAccount_isRejected() {
        User active = User.builder().email("an@gmail.com").enabled(true).build();
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> service.register(form("an@gmail.com", "0912345678")))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_again_beforeActivation_updatesUserAndReusesRecentCode() {
        User pending = User.builder().email("an@gmail.com").fullName("Cũ").enabled(false).build();
        pending.setId(5L);
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(pending));
        when(otpService.secondsUntilResend(pending, OtpType.REGISTER)).thenReturn(30L);

        service.register(form("an@gmail.com", "0912345678"));

        assertThat(pending.getFullName()).isEqualTo("Nguyễn Văn An");
        verify(otpService, never()).issue(any(), any());
    }

    @Test
    void register_rechecksPasswordRulesInService() {
        RegisterRequest weak = form("an@gmail.com", "0912345678");
        weak.setPassword("abc");
        weak.setConfirmPassword("abc");
        assertThatThrownBy(() -> service.register(weak)).isInstanceOf(BusinessException.class);

        RegisterRequest mismatch = form("an@gmail.com", "0912345678");
        mismatch.setConfirmPassword("Khac12345");
        assertThatThrownBy(() -> service.register(mismatch)).hasMessageContaining("không khớp");
    }

    @Test
    void activate_correctCode_enablesUser() {
        User pending = User.builder().email("an@gmail.com").enabled(false).build();
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(pending));

        service.activate("AN@gmail.com", "123456");

        verify(otpService).verify(pending, OtpType.REGISTER, "123456");
        assertThat(pending.isEnabled()).isTrue();
    }

    @Test
    void activate_wrongCode_keepsUserInactive() {
        User pending = User.builder().email("an@gmail.com").enabled(false).build();
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(pending));
        doThrow(new OtpException("sai")).when(otpService).verify(pending, OtpType.REGISTER, "000000");

        assertThatThrownBy(() -> service.activate("an@gmail.com", "000000")).isInstanceOf(OtpException.class);
        assertThat(pending.isEnabled()).isFalse();
    }

    @Test
    void activate_unknownEmail_givesGenericMessage() {
        assertThatThrownBy(() -> service.activate("ai-do@gmail.com", "123456"))
                .isInstanceOf(OtpException.class)
                .hasMessageNotContaining("không tồn tại");
    }

    @Test
    void resend_unknownEmail_doesNothingSilently() {
        service.resendActivationCode("ai-do@gmail.com");
        verify(otpService, never()).issue(any(), any());
    }

    private static RegisterRequest form(String email, String phone) {
        RegisterRequest form = new RegisterRequest();
        form.setFullName("Nguyễn Văn An");
        form.setEmail(email);
        form.setPhone(phone);
        form.setPassword("Starshop123");
        form.setConfirmPassword("Starshop123");
        return form;
    }
}
