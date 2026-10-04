package com.starshop.dto.shop;

import com.starshop.dto.RegisterRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form đăng ký mở shop và sửa thông tin shop. Logo, banner gửi riêng dạng file (không bắt buộc).
 */
@Getter
@Setter
@NoArgsConstructor
public class ShopForm {

    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_DESCRIPTION_LENGTH = 2000;

    @NotBlank(message = "Vui lòng nhập tên shop")
    @Size(min = 3, max = MAX_NAME_LENGTH, message = "Tên shop từ 3 đến 150 ký tự")
    private String name;

    @Size(max = MAX_DESCRIPTION_LENGTH, message = "Mô tả tối đa 2000 ký tự")
    private String description;

    @NotBlank(message = "Vui lòng nhập địa chỉ lấy hàng")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String pickupAddress;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = RegisterRequest.PHONE_REGEX, message = "Số điện thoại không hợp lệ (ví dụ 0912345678)")
    private String phone;
}
