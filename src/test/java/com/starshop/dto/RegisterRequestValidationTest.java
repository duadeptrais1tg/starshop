package com.starshop.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void validForm_hasNoErrors() {
        assertThat(errorFields(valid())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"0912345678", "0387654321", "+84912345678", "0701234567"})
    void acceptsVietnamesePhones(String phone) {
        RegisterRequest form = valid();
        form.setPhone(phone);
        assertThat(errorFields(form)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"091234567", "01234567890", "0212345678", "abc", "84912345678"})
    void rejectsInvalidPhones(String phone) {
        RegisterRequest form = valid();
        form.setPhone(phone);
        assertThat(errorFields(form)).containsExactly("phone");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc12", "abcdefgh", "12345678", "        "})
    void rejectsWeakPasswords(String password) {
        RegisterRequest form = valid();
        form.setPassword(password);
        form.setConfirmPassword(password);
        assertThat(errorFields(form)).contains("password");
    }

    @Test
    void acceptsVietnameseLettersInPassword() {
        RegisterRequest form = valid();
        form.setPassword("hoahồng2026");
        form.setConfirmPassword("hoahồng2026");
        assertThat(errorFields(form)).isEmpty();
    }

    @Test
    void mismatchedConfirmation_isReportedOnConfirmField() {
        RegisterRequest form = valid();
        form.setConfirmPassword("Khac12345");
        assertThat(errorFields(form)).containsExactly("confirmPassword");
    }

    @Test
    void rejectsBadEmailAndBlankName() {
        RegisterRequest form = valid();
        form.setEmail("khong-phai-email");
        form.setFullName(" ");
        assertThat(errorFields(form)).containsExactlyInAnyOrder("email", "fullName");
    }

    private static RegisterRequest valid() {
        RegisterRequest form = new RegisterRequest();
        form.setFullName("Nguyễn Văn An");
        form.setEmail("an@gmail.com");
        form.setPhone("0912345678");
        form.setPassword("Starshop123");
        form.setConfirmPassword("Starshop123");
        return form;
    }

    private static Set<String> errorFields(RegisterRequest form) {
        return validator.validate(form).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }
}
