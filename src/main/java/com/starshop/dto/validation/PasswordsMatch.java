package com.starshop.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Kiểm tra "nhập lại mật khẩu" khớp với "mật khẩu". Đặt trên class implements PasswordConfirmable;
 * lỗi được gắn vào trường confirmPassword để hiển thị ngay dưới ô đó.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PasswordsMatchValidator.class)
public @interface PasswordsMatch {

    String message() default "Mật khẩu nhập lại không khớp";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
