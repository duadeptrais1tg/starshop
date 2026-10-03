package com.starshop.dto.account;

import com.starshop.dto.RegisterRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form sửa thông tin cá nhân. Email là tên đăng nhập nên không cho sửa.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProfileForm {

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = RegisterRequest.PHONE_REGEX, message = "Số điện thoại không hợp lệ (ví dụ 0912345678)")
    private String phone;
}
