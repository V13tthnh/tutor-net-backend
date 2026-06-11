package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.*;
import java.util.Set;

public final class RolePermissionRequest {

    public record CreateRoleRequest(
            @NotBlank(message = "Tên role không được để trống")
            @Size(max = 100)
            String name,

            @NotBlank(message = "Slug không được để trống")
            @Size(max = 100)
            @Pattern(regexp = "^[a-z0-9_]+$", message = "Slug chỉ gồm chữ thường, số và dấu gạch dưới")
            String slug,

            String description
    ) {}

    public record UpdateRoleRequest(
            @NotBlank(message = "Tên role không được để trống")
            @Size(max = 100)
            String name,

            String description
            // Không cho phép đổi slug sau khi tạo — slug dùng trong code
    ) {}

    // Đồng bộ toàn bộ permissions của 1 role
    public record SyncPermissionsRequest(
            @NotNull(message = "Danh sách permissionIds không được null (dùng [] để xoá hết)")
            Set<Long> permissionIds
    ) {}

    // Thêm / gỡ 1 permission khỏi role
    public record TogglePermissionRequest(
            @NotNull(message = "permissionId không được để trống")
            Long permissionId
    ) {}

    public record CreatePermissionRequest(
            @NotBlank(message = "Tên permission không được để trống")
            @Size(max = 150)
            String name,

            @NotBlank(message = "Slug không được để trống")
            @Size(max = 150)
            @Pattern(regexp = "^[a-z0-9_]+:[a-z0-9_]+$",
                    message = "Slug phải theo dạng module:action, VD: user:read")
            String slug,

            @NotBlank(message = "Module không được để trống")
            @Size(max = 100)
            String module,

            @NotBlank(message = "Action không được để trống")
            @Size(max = 100)
            String action,

            String description
    ) {}

    public record UpdatePermissionRequest(
            @NotBlank(message = "Tên permission không được để trống")
            @Size(max = 150)
            String name,

            String description
            // Không cho đổi slug/module/action — slug dùng trong guard security
    ) {}
}

