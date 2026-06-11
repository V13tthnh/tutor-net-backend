package com.tutornet.tutor_net.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.lang.reflect.RecordComponent;

/**
 * Validator cho {@link PasswordMatch}.
 * Dùng reflection để đọc giá trị của 2 field theo tên được khai báo trong annotation,
 * nên hoạt động với bất kỳ record nào, không hardcode tên field.
 */
public class PasswordMatchValidator
        implements ConstraintValidator<PasswordMatch, Object> {

    private String passwordField;
    private String confirmPasswordField;
    private String message;

    @Override
    public void initialize(PasswordMatch annotation) {
        this.passwordField        = annotation.password();
        this.confirmPasswordField = annotation.confirmPassword();
        this.message              = annotation.message();
    }

    @Override
    public boolean isValid(Object obj, ConstraintValidatorContext ctx) {
        if (obj == null) return true;

        try {
            String password        = getFieldValue(obj, passwordField);
            String confirmPassword = getFieldValue(obj, confirmPasswordField);

            // Nếu 1 trong 2 null/blank → để @NotBlank xử lý, không báo lỗi trùng lặp
            if (password == null || confirmPassword == null) return true;

            boolean matches = password.equals(confirmPassword);

            if (!matches) {
                // Gắn lỗi vào field confirmPassword thay vì class-level
                // → message hiển thị đúng vị trí trong response
                ctx.disableDefaultConstraintViolation();
                ctx.buildConstraintViolationWithTemplate(message)
                        .addPropertyNode(confirmPasswordField)
                        .addConstraintViolation();
            }

            return matches;

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "@PasswordMatch: không tìm thấy field '%s' hoặc '%s' trong %s"
                            .formatted(passwordField, confirmPasswordField, obj.getClass().getSimpleName()),
                    e
            );
        }
    }

    private String getFieldValue(Object obj, String fieldName) throws Exception {
        var method = obj.getClass().getMethod(fieldName);
        Object value = method.invoke(obj);
        return value == null ? null : value.toString();
    }

}