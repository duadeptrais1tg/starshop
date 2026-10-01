package com.starshop.dto;

import com.starshop.dto.validation.PasswordConfirmable;
import com.starshop.dto.validation.PasswordsMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dữ liệu form đăng ký tài khoản.
 */
@Getter
@Setter
@NoArgsConstructor
@PasswordsMatch
public class RegisterRequest implements PasswordConfirmable {

    /** SĐT Việt Nam: 0xxxxxxxxx hoặc +84xxxxxxxxx, đầu số di động 3/5/7/8/9. */
    public static final String PHONE_REGEX = "^(0|\\+84)(3|5|7|8|9)\\d{8}$";

    /** Tối thiểu 8 ký tự, có ít nhất 1 chữ cái và 1 chữ số. */
    public static final String PASSWORD_REGEX = "^(?=.*\\p{L})(?=.*\\d).{8,100}$";

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email")
    @Email(message = "Email không đúng định dạng")
    @Size(max = 150, message = "Email tối đa 150 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = PHONE_REGEX, message = "Số điện thoại không hợp lệ (ví dụ 0912345678)")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu")
    @Pattern(regexp = PASSWORD_REGEX, message = "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số")
    private String password;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu")
    private String confirmPassword;
}
