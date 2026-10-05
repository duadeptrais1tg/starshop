package com.starshop.service.impl;

import com.starshop.config.VnpayProperties;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VnpayServiceImplTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final VnpayServiceImpl service = new VnpayServiceImpl(
            new VnpayProperties("TMN01", "TESTSECRET", "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
                    "http://localhost:8080/payment/vnpay/return", 15),
            Clock.fixed(LocalDateTime.of(2026, 10, 5, 9, 30).atZone(ZONE).toInstant(), ZONE));

    @Test
    void hmacSha512_matchesReferenceValue() {
        // Giá trị tính độc lập bằng Python: hmac.new(b'TESTSECRET', b'...', hashlib.sha512).hexdigest()
        assertThat(VnpayServiceImpl.hmacSha512("TESTSECRET", "vnp_Amount=10000000&vnp_TxnRef=PAY1"))
                .isEqualTo("2fddf4dd8dd31d1532790f34eed11e6ec276c498caa7ef086845cbbfc480e3222ad71be994c2d2613affb9c83b95bd8f867bc95a1e52556ac2e3a0f3d2e93f42");
    }

    @Test
    void paymentUrl_hasSortedSignedParams_andAmountTimes100() {
        String url = service.buildPaymentUrl("PAY123", new BigDecimal("2663000.00"), "1.2.3.4");

        assertThat(url).startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?vnp_Amount=266300000&");
        Map<String, String> params = parse(url);
        assertThat(params).containsEntry("vnp_TmnCode", "TMN01")
                .containsEntry("vnp_TxnRef", "PAY123")
                .containsEntry("vnp_CurrCode", "VND")
                .containsEntry("vnp_Version", "2.1.0")
                .containsEntry("vnp_IpAddr", "1.2.3.4")
                .containsEntry("vnp_CreateDate", "20261005093000")
                .containsEntry("vnp_ExpireDate", "20261005094500")
                .containsEntry("vnp_ReturnUrl", "http://localhost:8080/payment/vnpay/return");
        // Key sắp xếp tăng dần, chữ ký nằm cuối
        assertThat(params.keySet()).containsSubsequence("vnp_Amount", "vnp_Command", "vnp_CreateDate", "vnp_TxnRef");
        assertThat(service.verify(params)).isTrue();
    }

    @Test
    void verify_rejectsTamperedOrUnsignedCallback() {
        Map<String, String> params = parse(service.buildPaymentUrl("PAY123", new BigDecimal("100000"), "1.2.3.4"));

        Map<String, String> tampered = new LinkedHashMap<>(params);
        tampered.put("vnp_Amount", "100");
        assertThat(service.verify(tampered)).isFalse();

        Map<String, String> unsigned = new LinkedHashMap<>(params);
        unsigned.remove("vnp_SecureHash");
        assertThat(service.verify(unsigned)).isFalse();

        // vnp_SecureHashType và tham số không phải vnp_ không tham gia chữ ký; chữ ký hoa / thường đều được
        Map<String, String> extra = new LinkedHashMap<>(params);
        extra.put("vnp_SecureHashType", "HmacSHA512");
        extra.put("foo", "bar");
        extra.put("vnp_SecureHash", params.get("vnp_SecureHash").toUpperCase());
        assertThat(service.verify(extra)).isTrue();
    }

    @Test
    void notConfigured_isDisabled_andRejectsEverything() {
        VnpayServiceImpl empty = new VnpayServiceImpl(new VnpayProperties("", "", "x", "y", 0), Clock.systemDefaultZone());
        assertThat(empty.isEnabled()).isFalse();
        assertThat(empty.verify(Map.of("vnp_SecureHash", "abc"))).isFalse();
    }

    /** Tách query string thành Map đã decode (giống cách servlet đọc tham số callback). */
    private static Map<String, String> parse(String url) {
        Map<String, String> params = new LinkedHashMap<>();
        for (String pair : URI.create(url).getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
        }
        return params;
    }
}
