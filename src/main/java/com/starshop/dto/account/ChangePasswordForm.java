package com.starshop.dto.account;

import com.starshop.dto.RegisterRequest;
import com.starshop.dto.validation.PasswordConfirmable;
import com.starshop.dto.validation.PasswordsMatch;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Form đổi mật khẩu: bắt buộc nhập mật khẩu hiện tại.
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordsMatch
public class ChangePasswordForm implements PasswordConfirmable {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String currentPassword;

    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Pattern(regexp = RegisterRequest.PASSWORD_REGEX, message = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String confirmPassword;
}
