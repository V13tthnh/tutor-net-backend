package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.UserStatus;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tập hợp các DTO response liên quan đến User + Role.
 *
 * Phân cấp sử dụng:
 * ┌──────────────────────────────────────────────────────────┐
 * │  UserSummaryResponse  — danh sách users (ít field)       │
 * │  UserDetailResponse   — chi tiết 1 user (đầy đủ field)   │
 * │  RoleSummary          — thông tin role gọn (id/name/slug) │
 * │  UserRoleDetail       — assignment role kèm metadata     │
 * │  PageResponse<T>      — wrapper phân trang               │
 * └──────────────────────────────────────────────────────────┘
 */
public final class UserRoleResponse {

    // Permission tóm tắt (nhúng trong RoleResponse)
    public record PermissionSummary(
            Long id,
            String name,
            String slug,
            String module,
            String action
    ) {}

    // Role chi tiết (dùng trong /admin/roles)
    public record RoleResponse(
            Long id,
            String name,
            String slug,
            String description,
            Boolean isSystem,
            Instant createdAt,
            Set<PermissionSummary> permissions,
            long userCount             // Số user đang giữ role này
    ) {}

    public record PermissionResponse(
            Long id,
            String name,
            String slug,
            String module,
            String description,
            Instant createdAt,
            String action
    ) {}

    // Role tóm tắt (nhúng trong UserResponse)
    public record RoleSummary(
            Long id,
            String name,
            String slug
    ) {}

    // UserRole assignment chi tiết
    public record UserRoleDetail(
            Long id,
            RoleSummary role,
            String assignedByName,     // fullName người gán (null nếu tự đăng ký)
            Instant expiresAt,         // null = vĩnh viễn
            Instant createdAt
    ) {}

    // =========================================================================
    // UserDetailResponse — dùng cho chi tiết (GET /admin/users/{id}, POST, PUT)
    // =========================================================================

    /**
     * Full response của một user, trả về sau mọi thao tác ghi.
     *
     * Địa chỉ được tách 3 cấp (province / ward / address) từ cột address
     * trong DB thông qua AddressUtils.parse(), giúp frontend hiển thị
     * từng trường riêng mà không cần tự parse.
     */
    public record UserDetailResponse(
            Long              id,
            String            email,
            String            fullName,
            String            phone,
            String            avatarUrl,
            GenderType        gender,
            Integer           birthYear,

            // Địa chỉ tách 3 cấp từ cột address (DB lưu nối chuỗi)
            String            province,
            String            ward,
            String            address,

            String            hometownProvince,
            String            hometownWard,
            String            hometownAddress,

            Map<String, String> socialLinks,

            UserStatus        status,
            Boolean           isVerified,
            Instant           emailVerifiedAt,
            Instant           lastLoginAt,
            Integer           loginCount,

            Instant           createdAt,
            Instant           updatedAt,

            List<UserRoleDetail> roles
    ) {}

    // =========================================================================
    // UserSummaryResponse — dùng cho danh sách (GET /admin/users)
    // =========================================================================

    public record UserSummaryResponse(
            Long              id,
            String            email,
            String            fullName,
            String            phone,
            String            avatarUrl,
            UserStatus        status,
            Boolean           isVerified,
            Instant           createdAt,
            List<RoleSummary> roles
    ) {}

    // Wrapper phân trang
    public record PageResponse<T>(
            List<T> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean last
    ) {
        public static <T> PageResponse<T> empty(Pageable pageable) {
            return new PageResponse<>(
                    List.of(),
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    0L,
                    0,
                    true
            );
        }
    }
}
