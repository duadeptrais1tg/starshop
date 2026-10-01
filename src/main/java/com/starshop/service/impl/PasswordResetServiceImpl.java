package com.starshop.service.impl;

import com.starshop.dto.RegisterRequest;
import com.starshop.dto.ResetPasswordRequest;
import com.starshop.entity.User;
import com.starshop.entity.enums.OtpType;
import com.starshop.exception.BusinessException;
import com.starshop.exception.OtpException;
import com.starshop.repository.UserRepository;
import com.starshop.service.OtpService;
import com.starshop.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String INVALID_CODE_MESSAGE = "Mã OTP không hợp lệ. Vui lòng kiểm tra lại.";

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(normalize(email))
                .filter(User::isEnabled)
                .ifPresent(user -> {
                    if (otpService.secondsUntilResend(user, OtpType.RESET_PASSWORD) > 0) {
                        // Bấm gửi liên tục: bỏ qua lặng lẽ, mã cũ vẫn dùng được
                        log.info("Bỏ qua yêu cầu đặt lại mật khẩu (chưa đủ 60 giây): {}", user.getEmail());
                        return;
                    }
                    otpService.issue(user, OtpType.RESET_PASSWORD);
                });
    }

    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public void resetPassword(ResetPasswordRequest request) {
        // Kiểm tra lại ở service (không chỉ dựa vào @Valid ở controller)
        if (request.getPassword() == null || !request.getPassword().matches(RegisterRequest.PASSWORD_REGEX)) {
            throw new BusinessException("Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số");
        }
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }

        User user = userRepository.findByEmail(normalize(request.getEmail()))
                .filter(User::isEnabled)
                .orElseThrow(() -> new OtpException(INVALID_CODE_MESSAGE));
        otpService.verify(user, OtpType.RESET_PASSWORD, request.getCode());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        log.info("Đã đặt lại mật khẩu cho {}", user.getEmail());
    }

    private static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
