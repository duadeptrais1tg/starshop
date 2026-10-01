package com.starshop.dto;

import com.starshop.dto.validation.PasswordConfirmable;
import com.starshop.dto.validation.PasswordsMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dữ liệu form đặt lại mật khẩu (sau khi nhận OTP qua email).
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordsMatch
public class ResetPasswordRequest implements PasswordConfirmable {

    @NotBlank(message = "Thiếu email")
    @Email(message = "Email không đúng định dạng")
    private String email;

    @NotBlank(message = "Vui lòng nhập mã OTP")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
    private String code;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Pattern(regexp = RegisterRequest.PASSWORD_REGEX, message = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String confirmPassword;
}
