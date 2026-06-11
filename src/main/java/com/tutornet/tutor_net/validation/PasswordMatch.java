package com.tutornet.tutor_net.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Annotation dùng ở class-level để kiểm tra 2 field password khớp nhau.
 *
 * Cách dùng:
 * <pre>
 *   {@literal @}PasswordMatch(
 *       password        = "newPassword",
 *       confirmPassword = "newPasswordConfirm"
 *   )
 *   public record ChangePasswordRequest(...) {}
 * </pre>
 */
@Documented
@Constraint(validatedBy = PasswordMatchValidator.class)
@Target(ElementType.TYPE)          // áp dụng ở class / record level
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordMatch {

    /** Tên field chứa mật khẩu mới */
    String password() default "newPassword";

    /** Tên field chứa xác nhận mật khẩu */
    String confirmPassword() default "newPasswordConfirm";

    String message() default "Xác nhận mật khẩu không khớp";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
