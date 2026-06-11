package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.UserStatus;
import com.tutornet.tutor_net.validation.PasswordMatch;
import jakarta.validation.constraints.*;

import java.time.Year;
import java.util.List;
import java.util.Map;

public final class UserRequest {

    private static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^a-zA-Z\\d\\s])\\S+$";

    private static final String PASSWORD_MESSAGE =
            "Mật khẩu phải có ít nhất 1 chữ hoa, 1 chữ thường, 1 chữ số và 1 ký tự đặc biệt, không chứa khoảng trắng";

    private static final String EMAIL_STRICT_PATTERN =
            "^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";

    private static final String AVATAR_URL_PATTERN =
            "^https://[a-zA-Z0-9.\\-/_%+]+\\.(jpg|jpeg|png|gif|webp|svg)(\\?[\\w=&%.\\-]*)?$";

    @PasswordMatch(password = "password", confirmPassword = "confirmPassword")
    public record CreateAdminRequest(
            @NotBlank(message = "Họ tên không được để trống")
            @Size(min = 2, max = 200, message = "Họ tên phải từ 2 đến 200 ký tự")
            String fullName,

            @NotBlank(message = "Email không được để trống")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            @Email(message = "Email không đúng định dạng", regexp = EMAIL_STRICT_PATTERN)
            String email,

            @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
            @Pattern(
                    regexp = "^$|^[0-9+\\-\\s]{7,20}$",
                    message = "Số điện thoại không hợp lệ (7–20 ký tự, chỉ gồm số, +, -, khoảng trắng)"
            )
            String phone,

            GenderType gender,

            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String password,

            @NotBlank(message = "Xác nhận mật khẩu không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String confirmPassword,

            @NotNull(message = "Trạng thái không được để trống")
            UserStatus status,

            @NotEmpty(message = "Phải chọn ít nhất 1 role")
            List<Long> roleIds
    ){}

    public record UpdateAdminRequest(

            @NotBlank(message = "Họ tên không được để trống")
            @Size(min = 2, max = 200, message = "Họ tên phải từ 2 đến 200 ký tự")
            String fullName,

            @NotBlank(message = "Email không được để trống")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            @Email(message = "Email không đúng định dạng", regexp = EMAIL_STRICT_PATTERN)
            String email,

            @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
            @Pattern(
                    regexp = "^$|^[0-9+\\-\\s]{7,20}$",
                    message = "Số điện thoại không hợp lệ (7–20 ký tự, chỉ gồm số, +, -, khoảng trắng)"
            )
            String phone,

            GenderType gender,

            @Min(1900)
            @Max(Year.MAX_VALUE)
            Integer birthYear,

            @NotBlank(message = "Tỉnh/Thành không được để trống")
            @Size(max = 100, message = "Tỉnh/Thành không được vượt quá 100 ký tự")
            String province,

            @NotBlank(message = "Xã/Phường không được để trống")
            @Size(max = 100, message = "Xã/Phường không được vượt quá 100 ký tự")
            String ward,

            @NotBlank(message = "Địa chỉ không được để trống")
            @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
            String address,

            String hometownProvince,
            String hometownWard,
            String hometownAddress,

            Map<String, String> socialLinks,

            // Tuỳ chọn — chỉ validate nếu được gửi lên
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String password,

            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String confirmPassword,

            @NotNull(message = "Trạng thái không được để trống")
            UserStatus status,

            @NotEmpty(message = "Phải chọn ít nhất 1 role")
            List<Long> roleIds

    ) {}

    public record UpdateProfileRequest(

            @NotBlank(message = "Họ tên không được để trống")
            @Size(min = 2, max = 200, message = "Họ tên phải từ 2 đến 200 ký tự")
            String fullName,

            @Size(max = 20, message = "Số điện thoại không được vượt quá 20 ký tự")
            @Pattern(
                    regexp = "^$|^[0-9+\\-\\s]{7,20}$",
                    message = "Số điện thoại không hợp lệ (7–20 ký tự, chỉ gồm số, +, -, khoảng trắng)"
            )
            String phone,

            GenderType gender,

            @Min(1900)
            @Max(Year.MAX_VALUE)
            Integer birthYear,

            @NotBlank(message = "Tỉnh/Thành không được để trống")
            @Size(max = 100, message = "Tỉnh/Thành không được vượt quá 100 ký tự")
            String province,

            @NotBlank(message = "Xã/Phường không được để trống")
            @Size(max = 100, message = "Xã/Phường không được vượt quá 100 ký tự")
            String ward,

            @NotBlank(message = "Địa chỉ không được để trống")
            @Size(max = 500, message = "Địa chỉ không được vượt quá 500 ký tự")
            String address,

            String hometownProvince,
            String hometownWard,
            String hometownAddress,


            // null = không thay đổi; empty map = xoá hết links
            Map<String, String> socialLinks

    ) {}

    @PasswordMatch(
            password        = "newPassword",
            confirmPassword = "confirmPassword",
            message         = "Xác nhận mật khẩu mới không khớp"
    )
    public record ResetPasswordRequest(

            @NotBlank(message = "Mật khẩu hiện tại không được để trống")
            String password,

            @NotBlank(message = "Mật khẩu mới không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu mới phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String newPassword,

            @NotBlank(message = "Xác nhận mật khẩu mới không được để trống")
            @Size(min = 8, max = 100, message = "Mật khẩu phải từ 8 đến 100 ký tự")
            @Pattern(regexp = PASSWORD_PATTERN, message = PASSWORD_MESSAGE)
            String confirmPassword

    ) {}

    public record UpdateAvatarRequest(
            @Size(max = 2048, message = "URL avatar không được vượt quá 2048 ký tự")
            @Pattern(
                    regexp = "^$|" + AVATAR_URL_PATTERN,
                    message = "Avatar phải là URL https:// hợp lệ với định dạng ảnh (jpg, jpeg, png, gif, webp, svg)"
            )
            String avatarUrl
    ) {}

    public record UpdateStatusRequest(
            @NotNull(message = "Trạng thái không được để trống")
            UserStatus status
    ) {}

    public record AssignRoleRequest(
            @NotNull(message = "roleId không được để trống")
            Long roleId
    ) {}
}