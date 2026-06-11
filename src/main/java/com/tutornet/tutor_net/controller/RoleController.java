package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.AssignUserRoleRequest;
import com.tutornet.tutor_net.dto.request.RolePermissionRequest.*;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import com.tutornet.tutor_net.service.RoleService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('role:read', 'role:create', 'role:update', 'role:delete')")
public class RoleController {

    private final RoleService roleService;

    // ─── Role CRUD ────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<PageResponse<RoleResponse>>> getRoles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isSystem,
            @RequestParam(defaultValue = "1")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(defaultValue = "asc")  String sortDir
    ) {
        Pageable pageable = PageableUtils.build(page, size, null, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok(
                roleService.getRoles(keyword, isSystem, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('role:read')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getRoleById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('role:create')")
    public ResponseEntity<ApiResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(roleService.createRole(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('role:update')")
    public ResponseEntity<ApiResponse<RoleResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật role thành công",
                roleService.updateRole(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('role:delete')")
    public ResponseEntity<ApiResponse<Void>> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.ok(ApiResponse.noContent("Xoá role thành công"));
    }

    // ─── Role ↔ Permission ────────────────────────────────────────────────────

    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:read_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> getRolePermissions(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roleService.getRoleById(id)));
    }

    // Thay thế toàn bộ permissions của role (dùng cho drag-drop UI)
    @PutMapping("/{id}/permissions/sync")
    @PreAuthorize("hasAuthority('role:assign_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> syncPermissions(
            @PathVariable Long id,
            @Valid @RequestBody SyncPermissionsRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Đồng bộ permissions thành công",
                roleService.syncPermissions(id, request)));
    }

    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('role:assign_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> addPermission(
            @PathVariable Long id,
            @Valid @RequestBody TogglePermissionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Thêm permission thành công",
                roleService.addPermission(id, request)));
    }

    @DeleteMapping("/{id}/permissions/{permissionId}")
    @PreAuthorize("hasAuthority('role:assign_permission')")
    public ResponseEntity<ApiResponse<RoleResponse>> removePermission(
            @PathVariable Long id,
            @PathVariable Long permissionId
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Gỡ permission thành công",
                roleService.removePermission(id, permissionId)));
    }

    // ─── Role ↔ User ──────────────────────────────────────────────────────────

    @GetMapping("/{id}/users")
    @PreAuthorize("hasAuthority('role:read_user')")
    public ResponseEntity<ApiResponse<PageResponse<UserSummaryResponse>>> getUsersByRole(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageableUtils.build(page, size, null, "createdAt", "desc");
        return ResponseEntity.ok(ApiResponse.ok(
                roleService.getUsersByRole(id, pageable)));
    }

    @PostMapping("/{id}/users")
    @PreAuthorize("hasAuthority('role:assign_user')")
    public ResponseEntity<ApiResponse<Void>> assignUserToRole(
            @PathVariable Long id,
            @Valid @RequestBody AssignUserRoleRequest request
    ) {
        roleService.assignUserToRole(id, request);
        return ResponseEntity.ok(ApiResponse.noContent("Gán vai trò cho user thành công"));
    }

    @DeleteMapping("/{id}/users/{userId}")
    @PreAuthorize("hasAuthority('role:revoke_user')")
    public ResponseEntity<ApiResponse<Void>> revokeUserFromRole(
            @PathVariable Long id,
            @PathVariable Long userId
    ) {
        roleService.revokeUserFromRole(id, userId);
        return ResponseEntity.ok(ApiResponse.noContent("Thu hồi vai trò khỏi user thành công"));
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('role:export')")
    public ResponseEntity<ApiResponse<Void>> exportRoles() {
        roleService.exportRoles();
        return ResponseEntity.ok(ApiResponse.noContent("Xuất danh sách role thành công"));
    }
}