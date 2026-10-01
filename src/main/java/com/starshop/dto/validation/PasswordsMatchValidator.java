package com.starshop.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Objects;

public class PasswordsMatchValidator implements ConstraintValidator<PasswordsMatch, PasswordConfirmable> {

    @Override
    public boolean isValid(PasswordConfirmable value, ConstraintValidatorContext context) {
        if (value == null || value.getConfirmPassword() == null || value.getConfirmPassword().isEmpty()) {
            // Ô trống đã có @NotBlank báo lỗi riêng
            return true;
        }
        if (Objects.equals(value.getPassword(), value.getConfirmPassword())) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("confirmPassword")
                .addConstraintViolation();
        return false;
    }
}
