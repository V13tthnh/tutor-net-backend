package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.EnumOptionRequest;
import com.tutornet.tutor_net.dto.request.UserRequest.*;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.UserFilterOptionsResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import com.tutornet.tutor_net.enums.UserStatus;
import com.tutornet.tutor_net.service.UserService;
import com.tutornet.tutor_net.service.impl.FileStorageServiceImpl;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final FileStorageServiceImpl fileStorageService;

    @GetMapping
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<UserStatus> status,
            @RequestParam(required = false) List<String> roles,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableUtils.build(page, size, limit, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok(
                userService.getUsers(keyword, status, roles, pageable)));
    }

    @GetMapping("/filter-options")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserFilterOptionsResponse>> getFilterOptions() {
        return ResponseEntity.ok(ApiResponse.ok(userService.getFilterOptions()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('user:read')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getUserById(id)));
    }

    @GetMapping("/statuses")
    public List<EnumOptionRequest> getStatuses() {
        return Arrays.stream(UserStatus.values())
                .map(status -> new EnumOptionRequest(
                        status.name(),
                        status.getLabel()
                ))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasAuthority('user:create')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> createAdmin(
            @Valid @RequestBody CreateAdminRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(userService.createAdmin(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateAdmin(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAdminRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", userService.updateAdmin(id, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('user:suspend')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái thành công",
                userService.updateStatus(id, request)));
    }

    @PatchMapping("/{id}/avatar")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<Void>> updateAvatar(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String filePath = fileStorageService.storeAvatar(file); // /avatars/xxx.png
        userService.updateAvatar(id, new UpdateAvatarRequest(filePath)); // lưu path
        return ResponseEntity.ok(ApiResponse.noContent("Cập nhật Avatar thành công"));
    }

    @PatchMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('user:update')")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        userService.resetPassword(id, request);
        return ResponseEntity.ok(ApiResponse.noContent("Đặt lại mật khẩu thành công"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('user:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.noContent("Xoá tài khoản thành công"));
    }

    @PostMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('user:assign_role')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> assignRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignRoleRequest request,
            @AuthenticationPrincipal UserDetails currentUser
            // currentUser.getUsername() → lấy email → resolve thành userId trong security config
    ) {

        Long assignedById = resolveCurrentUserId(currentUser);
        return ResponseEntity.ok(ApiResponse.ok("Gán role thành công",
                userService.assignRole(id, request, assignedById)));
    }

    @DeleteMapping("/{id}/roles/{roleId}")
    @PreAuthorize("hasAuthority('user:assign_role')")
    public ResponseEntity<ApiResponse<UserDetailResponse>> revokeRole(
            @PathVariable Long id,
            @PathVariable Long roleId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Gỡ role thành công",
                userService.revokeRole(id, roleId)));
    }

    private Long resolveCurrentUserId(UserDetails userDetails) {
        if (userDetails instanceof com.tutornet.tutor_net.security.CustomUserDetails) {
            return ((com.tutornet.tutor_net.security.CustomUserDetails) userDetails).getUser().getId();
        }
        throw new IllegalStateException("UserDetails không hợp lệ");
    }
}
