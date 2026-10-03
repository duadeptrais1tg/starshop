package com.starshop.dto.account;

import com.starshop.dto.RegisterRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form thêm / sửa địa chỉ nhận hàng.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddressForm {

    @NotBlank(message = "Vui lòng nhập tên người nhận")
    @Size(max = 100, message = "Tên người nhận tối đa 100 ký tự")
    private String receiverName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = RegisterRequest.PHONE_REGEX, message = "Số điện thoại không hợp lệ (ví dụ 0912345678)")
    private String phone;

    @NotBlank(message = "Vui lòng nhập tỉnh / thành phố")
    @Size(max = 100, message = "Tối đa 100 ký tự")
    private String province;

    @NotBlank(message = "Vui lòng nhập quận / huyện")
    @Size(max = 100, message = "Tối đa 100 ký tự")
    private String district;

    @NotBlank(message = "Vui lòng nhập phường / xã")
    @Size(max = 100, message = "Tối đa 100 ký tự")
    private String ward;

    @NotBlank(message = "Vui lòng nhập địa chỉ chi tiết (số nhà, tên đường)")
    @Size(max = 255, message = "Địa chỉ chi tiết tối đa 255 ký tự")
    private String detail;

    private boolean defaultAddress;
}
