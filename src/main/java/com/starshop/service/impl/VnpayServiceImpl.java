package com.starshop.service.impl;

import com.starshop.config.VnpayProperties;
import com.starshop.service.VnpayService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

@Service
@RequiredArgsConstructor
public class VnpayServiceImpl implements VnpayService {

    static final String SECURE_HASH = "vnp_SecureHash";
    static final String SECURE_HASH_TYPE = "vnp_SecureHashType";
    private static final ZoneId VN_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNP_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;
    private final Clock clock;

    @Override
    public boolean isEnabled() {
        return properties.isConfigured();
    }

    @Override
    public String buildPaymentUrl(String txnRef, BigDecimal amount, String clientIp) {
        if (!isEnabled()) {
            throw new IllegalStateException("Chưa cấu hình VNPAY (VNPAY_TMN_CODE / VNPAY_HASH_SECRET)");
        }
        ZonedDateTime now = ZonedDateTime.now(clock).withZoneSameInstant(VN_ZONE);
        Map<String, String> params = new TreeMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", properties.tmnCode());
        // Số tiền nhân 100 (VNPAY không dùng phần thập phân)
        params.put("vnp_Amount", amount.movePointRight(2).toBigInteger().toString());
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", txnRef);
        // Không dấu, không ký tự đặc biệt (theo yêu cầu của VNPAY)
        params.put("vnp_OrderInfo", "Thanh toan don hang StarShop " + txnRef);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", properties.returnUrl());
        params.put("vnp_IpAddr", clientIp == null || clientIp.isBlank() ? "127.0.0.1" : clientIp);
        params.put("vnp_CreateDate", now.format(VNP_TIME));
        params.put("vnp_ExpireDate", now.plusMinutes(properties.expireMinutes()).format(VNP_TIME));

        String query = encode(params);
        return properties.payUrl() + "?" + query + "&" + SECURE_HASH + "=" + hmacSha512(properties.hashSecret(), query);
    }

    @Override
    public boolean verify(Map<String, String> params) {
        String received = params.get(SECURE_HASH);
        if (!isEnabled() || received == null || received.isBlank()) {
            return false;
        }
        Map<String, String> fields = new TreeMap<>();
        params.forEach((key, value) -> {
            if (key.startsWith("vnp_") && !SECURE_HASH.equals(key) && !SECURE_HASH_TYPE.equals(key)
                    && value != null && !value.isEmpty()) {
                fields.put(key, value);
            }
        });
        String expected = hmacSha512(properties.hashSecret(), encode(fields));
        // So sánh thời gian hằng (tránh dò chữ ký theo thời gian phản hồi)
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.US_ASCII),
                received.toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    /** key=value nối bằng &, sắp xếp theo key, giá trị URL-encode – đúng cách VNPAY tạo chuỗi ký. */
    static String encode(Map<String, String> sortedParams) {
        StringJoiner joiner = new StringJoiner("&");
        sortedParams.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                joiner.add(URLEncoder.encode(key, StandardCharsets.US_ASCII) + "="
                        + URLEncoder.encode(value, StandardCharsets.US_ASCII));
            }
        });
        return joiner.toString();
    }

    static String hmacSha512(String secret, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Không tạo được chữ ký HMAC-SHA512", e);
        }
    }
}
