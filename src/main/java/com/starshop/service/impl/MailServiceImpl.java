package com.starshop.service.impl;

import com.starshop.config.AppMailProperties;
import com.starshop.entity.enums.OtpType;
import com.starshop.service.MailService;
import com.starshop.service.event.OtpIssuedEvent;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Gửi email HTML qua Spring Mail (SMTP). Được gọi bất đồng bộ từ OtpEmailListener.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MailServiceImpl implements MailService {

    private static final String OTP_TEMPLATE = "mail/otp-email.html";

    private final JavaMailSender mailSender;
    private final MailProperties smtpProperties;
    private final AppMailProperties appMailProperties;

    @Override
    public void sendOtpEmail(OtpIssuedEvent event) {
        if (!StringUtils.hasText(smtpProperties.getUsername())) {
            if (appMailProperties.devLogOtp()) {
                log.warn("[DEV] Chưa cấu hình SMTP - OTP {} của {}: {}", event.type(), event.email(), event.code());
            } else {
                log.error("Chưa cấu hình SMTP (MAIL_USERNAME / MAIL_PASSWORD), không gửi được email OTP tới {}", event.email());
            }
            return;
        }
        boolean register = event.type() == OtpType.REGISTER;
        String subject = register ? "Mã kích hoạt tài khoản StarShop" : "Mã đặt lại mật khẩu StarShop";
        String html = render(loadTemplate(), Map.of(
                "title", register ? "Kích hoạt tài khoản" : "Đặt lại mật khẩu",
                "intro", register
                        ? "Cảm ơn bạn đã đăng ký StarShop. Nhập mã dưới đây để kích hoạt tài khoản:"
                        : "Bạn vừa yêu cầu đặt lại mật khẩu. Nhập mã dưới đây để tiếp tục:",
                "fullName", HtmlUtils.htmlEscape(event.fullName()),
                "code", event.code(),
                "minutes", String.valueOf(event.validMinutes())));
        try {
            send(event.email(), subject, html);
            log.info("Đã gửi email OTP {} tới {}", event.type(), event.email());
        } catch (MailException | MessagingException | IOException e) {
            // Gửi bất đồng bộ nên không báo lỗi về trình duyệt được; người dùng bấm "Gửi lại mã"
            log.error("Gửi email OTP tới {} thất bại: {}", event.email(), e.getMessage());
        }
    }

    private void send(String to, String subject, String html) throws MessagingException, IOException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());
        String from = StringUtils.hasText(appMailProperties.from()) ? appMailProperties.from() : smtpProperties.getUsername();
        String fromName = StringUtils.hasText(appMailProperties.fromName()) ? appMailProperties.fromName() : "StarShop";
        helper.setFrom(from, fromName);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(message);
    }

    private static String loadTemplate() {
        try {
            return new ClassPathResource(OTP_TEMPLATE).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Không đọc được template email " + OTP_TEMPLATE, e);
        }
    }

    /** Thay {{key}} trong template bằng giá trị (giá trị do người dùng nhập phải được escape trước). */
    static String render(String template, Map<String, String> values) {
        String result = template;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }
}
