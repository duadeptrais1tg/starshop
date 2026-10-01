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
import com.starshop.service.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserRegistrationServiceImpl implements UserRegistrationService {

    /** Thông báo chung khi không tìm thấy email: không tiết lộ email có tồn tại hay không. */
    private static final String INVALID_CODE_MESSAGE = "Mã OTP không hợp lệ. Vui lòng kiểm tra lại.";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OtpService otpService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        validate(request);
        String email = normalizeEmail(request.getEmail());

        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent() && existing.get().isEnabled()) {
            throw new EmailAlreadyExistsException();
        }
        // Email đã đăng ký nhưng chưa kích hoạt: cho đăng ký lại (cập nhật thông tin, gửi mã mới).
        // Người khác đăng ký "giữ chỗ" email của bạn cũng không kích hoạt được vì không có mã trong hộp thư.
        User user = existing.orElseGet(User::new);
        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(normalizePhone(request.getPhone()));
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(false);
        user.setRoles(new HashSet<>(Set.of(userRole())));
        user = userRepository.save(user);

        // Vừa gửi mã chưa đủ 60 giây (bấm đăng ký 2 lần) thì dùng lại mã cũ, không báo lỗi
        if (otpService.secondsUntilResend(user, OtpType.REGISTER) == 0) {
            otpService.issue(user, OtpType.REGISTER);
        }
        return email;
    }

    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public void activate(String email, String code) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new OtpException(INVALID_CODE_MESSAGE));
        if (user.isEnabled()) {
            throw new BusinessException("Tài khoản đã được kích hoạt, vui lòng đăng nhập.");
        }
        otpService.verify(user, OtpType.REGISTER, code);
        user.setEnabled(true);
    }

    @Override
    @Transactional
    public void resendActivationCode(String email) {
        Optional<User> user = userRepository.findByEmail(normalizeEmail(email));
        if (user.isEmpty()) {
            // Không báo "email không tồn tại" để tránh dò email
            return;
        }
        if (user.get().isEnabled()) {
            throw new BusinessException("Tài khoản đã được kích hoạt, vui lòng đăng nhập.");
        }
        otpService.issue(user.get(), OtpType.REGISTER);
    }

    @Override
    @Transactional(readOnly = true)
    public long secondsUntilResend(String email) {
        return userRepository.findByEmail(normalizeEmail(email))
                .map(user -> otpService.secondsUntilResend(user, OtpType.REGISTER))
                .orElse(0L);
    }

    /** Kiểm tra lại ở service (không tin hoàn toàn vào validate của controller). */
    private static void validate(RegisterRequest request) {
        if (request.getPassword() == null || !request.getPassword().matches(RegisterRequest.PASSWORD_REGEX)) {
            throw new BusinessException("Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số");
        }
        if (!Objects.equals(request.getPassword(), request.getConfirmPassword())) {
            throw new BusinessException("Mật khẩu nhập lại không khớp");
        }
        if (request.getPhone() == null || !request.getPhone().trim().matches(RegisterRequest.PHONE_REGEX)) {
            throw new BusinessException("Số điện thoại không hợp lệ");
        }
        if (request.getFullName() == null || request.getFullName().isBlank()) {
            throw new BusinessException("Vui lòng nhập họ tên");
        }
    }

    private Role userRole() {
        return roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("Thiếu role USER trong CSDL"));
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    /** +84912345678 -> 0912345678 */
    static String normalizePhone(String phone) {
        String trimmed = phone.trim();
        return trimmed.startsWith("+84") ? "0" + trimmed.substring(3) : trimmed;
    }
}
