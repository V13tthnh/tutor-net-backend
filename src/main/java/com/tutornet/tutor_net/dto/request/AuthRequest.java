package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.validation.PasswordMatch;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthRequest {

    private AuthRequest() {
    }

    /**
     * Regex giải thích:
     *  (?=.*[a-z])        — ít nhất 1 chữ thường
     *  (?=.*[A-Z])        — ít nhất 1 chữ hoa
     *  (?=.*\d)           — ít nhất 1 chữ số
     *  (?=.*[^a-zA-Z\d\s])— ít nhất 1 ký tự đặc biệt
     *  \S+                — không chứa khoảng trắng
     */
    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s])\\S+$";

    private static final String PASSWORD_MESSAGE =
            "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt, không chứa khoảng trắng";

    /**
     * Regex email bổ sung cho @Email
     */
    private static final String EMAIL_STRICT_PATTERN =
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    public record LoginRequest(

            @NotBlank(message = "Email không được để trống")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            @Email(
                    message = "Email không đúng định dạng",
                    regexp = EMAIL_STRICT_PATTERN
            )
            String email,

            @NotBlank(message = "Mật khẩu không được để trống")
            String password

    ) {
    }

    @PasswordMatch(
            password = "password",
            confirmPassword = "confirmPassword"
    )
    public record RegisterRequest(

            @NotBlank(message = "Họ tên không được để trống")
            @Size(min = 2, max = 200, message = "Họ tên phải từ 2 đến 200 ký tự")
            String fullName,


            @NotBlank(message = "Email không được để trống")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            @Email(
                    message = "Email không đúng định dạng",
                    regexp = EMAIL_STRICT_PATTERN
            )
            String email,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String password,

            @NotBlank(message = "Xác nhận mật khẩu không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String confirmPassword

    ) {}

    public record RefreshTokenRequest(
            String refreshToken
    ){}

    public record LogoutRequest(
            @NotBlank(message = "Refresh token không được để trống")
            String refreshToken
    ){}

    public record ForgotPasswordRequest(

            @NotBlank(message = "Email không được để trống")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            @Email(
                    message = "Email không đúng định dạng",
                    regexp = EMAIL_STRICT_PATTERN
            )
            String email

    ) {}

    @PasswordMatch(
            password = "newPassword",
            confirmPassword = "confirmPassword"
    )
    public record ResetPasswordRequest(

            @NotBlank(message = "Token không được để trống")
            String token,

            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword,

            @NotBlank(message = "Xác nhận mật khẩu không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String confirmPassword

    ) {}
}