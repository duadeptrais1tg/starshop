package com.starshop.dto.account;

import lombok.Builder;
import lombok.Getter;

/**
 * Địa chỉ nhận hàng hiển thị cho chủ tài khoản.
 */
@Getter
@Builder
public class AddressDto {

    private final Long id;
    private final String receiverName;
    private final String phone;
    private final String province;
    private final String district;
    private final String ward;
    private final String detail;
    private final boolean defaultAddress;

    /** "12 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. Hồ Chí Minh" */
    public String getFullAddress() {
        return String.join(", ", detail, ward, district, province);
    }
}
