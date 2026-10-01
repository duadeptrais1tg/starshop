package com.starshop.service.impl;

import com.starshop.entity.OtpToken;
import com.starshop.entity.User;
import com.starshop.entity.enums.OtpType;
import com.starshop.exception.OtpException;
import com.starshop.repository.OtpTokenRepository;
import com.starshop.service.OtpService;
import com.starshop.service.event.OtpIssuedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    static final Duration VALIDITY = Duration.ofMinutes(5);
    static final Duration RESEND_COOLDOWN = Duration.ofSeconds(60);
    static final int MAX_FAILED_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpTokenRepository otpTokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    @Override
    @Transactional
    public void issue(User user, OtpType type) {
        long wait = secondsUntilResend(user, type);
        if (wait > 0) {
            throw new OtpException("Vui lòng đợi " + wait + " giây trước khi gửi lại mã.");
        }
        otpTokenRepository.invalidateActive(user.getId(), type);

        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        otpTokenRepository.save(OtpToken.builder()
                .user(user)
                .code(code)
                .type(type)
                .expiresAt(now().plus(VALIDITY))
                .build());

        eventPublisher.publishEvent(new OtpIssuedEvent(
                user.getEmail(), user.getFullName(), code, type, VALIDITY.toMinutes()));
    }

    /**
     * noRollbackFor: khi nhập sai vẫn phải LƯU số lần sai rồi mới báo lỗi;
     * nếu rollback thì bộ đếm không bao giờ tăng và giới hạn 5 lần vô tác dụng.
     */
    @Override
    @Transactional(noRollbackFor = OtpException.class)
    public void verify(User user, OtpType type, String code) {
        OtpToken token = otpTokenRepository.findTopByUserIdAndTypeOrderByIdDesc(user.getId(), type)
                .filter(t -> !t.isUsed())
                .orElseThrow(() -> new OtpException("Mã OTP không hợp lệ. Vui lòng gửi lại mã mới."));

        if (token.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            throw new OtpException("Bạn đã nhập sai quá " + MAX_FAILED_ATTEMPTS + " lần. Vui lòng gửi lại mã mới.");
        }
        if (token.getExpiresAt().isBefore(now())) {
            throw new OtpException("Mã OTP đã hết hạn. Vui lòng gửi lại mã mới.");
        }
        if (!sameCode(token.getCode(), code)) {
            token.setFailedAttempts(token.getFailedAttempts() + 1);
            otpTokenRepository.save(token);
            int remaining = MAX_FAILED_ATTEMPTS - token.getFailedAttempts();
            throw new OtpException(remaining > 0
                    ? "Mã OTP không đúng. Bạn còn " + remaining + " lần thử."
                    : "Mã OTP không đúng. Bạn đã hết lượt thử, vui lòng gửi lại mã mới.");
        }

        token.setUsed(true);
        otpTokenRepository.save(token);
    }

    @Override
    @Transactional(readOnly = true)
    public long secondsUntilResend(User user, OtpType type) {
        if (user.getId() == null) {
            return 0;
        }
        return otpTokenRepository.findTopByUserIdAndTypeOrderByIdDesc(user.getId(), type)
                .map(t -> Duration.between(now(), t.getCreatedAt().plus(RESEND_COOLDOWN)).toSeconds())
                .filter(seconds -> seconds > 0)
                .orElse(0L);
    }

    /** So sánh thời gian hằng (constant-time) để không đoán được mã qua thời gian phản hồi. */
    private static boolean sameCode(String expected, String actual) {
        if (actual == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                actual.trim().getBytes(StandardCharsets.UTF_8));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
