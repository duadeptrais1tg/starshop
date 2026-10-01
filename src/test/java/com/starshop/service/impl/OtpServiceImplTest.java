package com.starshop.service.impl;

import com.starshop.entity.OtpToken;
import com.starshop.entity.User;
import com.starshop.entity.enums.OtpType;
import com.starshop.exception.OtpException;
import com.starshop.repository.OtpTokenRepository;
import com.starshop.service.event.OtpIssuedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OtpServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 1, 9, 0, 0);

    private final OtpTokenRepository repository = mock(OtpTokenRepository.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final OtpServiceImpl service =
            new OtpServiceImpl(repository, publisher, Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE));
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().email("an@starshop.vn").fullName("An").password("x").build();
        user.setId(10L);
        when(repository.findTopByUserIdAndTypeOrderByIdDesc(10L, OtpType.REGISTER)).thenReturn(Optional.empty());
    }

    // ------------------------------------------------------------------ issue

    @Test
    void issue_savesSixDigitCodeValidFiveMinutes_andPublishesEvent() {
        service.issue(user, OtpType.REGISTER);

        ArgumentCaptor<OtpToken> saved = ArgumentCaptor.forClass(OtpToken.class);
        verify(repository).invalidateActive(10L, OtpType.REGISTER);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getCode()).matches("\\d{6}");
        assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plusMinutes(5));

        ArgumentCaptor<OtpIssuedEvent> event = ArgumentCaptor.forClass(OtpIssuedEvent.class);
        verify(publisher).publishEvent(event.capture());
        assertThat(event.getValue().code()).isEqualTo(saved.getValue().getCode());
        assertThat(event.getValue().email()).isEqualTo("an@starshop.vn");
    }

    @Test
    void issue_within60Seconds_isRejected() {
        givenLatest(token("123456", NOW.minusSeconds(20)));

        assertThat(service.secondsUntilResend(user, OtpType.REGISTER)).isEqualTo(40);
        assertThatThrownBy(() -> service.issue(user, OtpType.REGISTER))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("40 giây");
        verify(repository, never()).save(any());
    }

    @Test
    void issue_after60Seconds_isAllowed() {
        givenLatest(token("123456", NOW.minusSeconds(61)));

        assertThat(service.secondsUntilResend(user, OtpType.REGISTER)).isZero();
        service.issue(user, OtpType.REGISTER);
        verify(repository).save(any());
    }

    // ----------------------------------------------------------------- verify

    @Test
    void verify_correctCode_marksUsed() {
        OtpToken token = givenLatest(token("123456", NOW.minusMinutes(1)));

        service.verify(user, OtpType.REGISTER, "123456");

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void verify_wrongCode_countsAttempts_thenLocksAfterFive() {
        OtpToken token = givenLatest(token("123456", NOW.minusMinutes(1)));

        for (int i = 1; i <= 4; i++) {
            assertThatThrownBy(() -> service.verify(user, OtpType.REGISTER, "000000"))
                    .hasMessageContaining("còn " + (5 - token.getFailedAttempts()) + " lần");
        }
        assertThatThrownBy(() -> service.verify(user, OtpType.REGISTER, "000000"))
                .hasMessageContaining("hết lượt");
        assertThat(token.getFailedAttempts()).isEqualTo(5);

        // Đã sai 5 lần thì kể cả nhập đúng cũng bị từ chối
        assertThatThrownBy(() -> service.verify(user, OtpType.REGISTER, "123456"))
                .hasMessageContaining("quá 5 lần");
        assertThat(token.isUsed()).isFalse();
    }

    @Test
    void verify_expiredCode_isRejected() {
        OtpToken token = token("123456", NOW.minusMinutes(6));
        token.setExpiresAt(NOW.minusMinutes(1));
        givenLatest(token);

        assertThatThrownBy(() -> service.verify(user, OtpType.REGISTER, "123456"))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("hết hạn");
    }

    @Test
    void verify_usedCodeCannotBeReused() {
        OtpToken token = givenLatest(token("123456", NOW.minusMinutes(1)));
        token.setUsed(true);

        assertThatThrownBy(() -> service.verify(user, OtpType.REGISTER, "123456"))
                .isInstanceOf(OtpException.class)
                .hasMessageContaining("không hợp lệ");
    }

    // ---------------------------------------------------------------- helpers

    private OtpToken givenLatest(OtpToken token) {
        when(repository.findTopByUserIdAndTypeOrderByIdDesc(10L, OtpType.REGISTER)).thenReturn(Optional.of(token));
        return token;
    }

    private OtpToken token(String code, LocalDateTime createdAt) {
        OtpToken token = OtpToken.builder()
                .user(user)
                .code(code)
                .type(OtpType.REGISTER)
                .expiresAt(createdAt.plusMinutes(5))
                .build();
        token.setCreatedAt(createdAt);
        return token;
    }
}
