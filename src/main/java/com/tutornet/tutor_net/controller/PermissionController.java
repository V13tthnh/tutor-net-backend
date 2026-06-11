package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.RolePermissionRequest.*;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.PermissionGroupResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import com.tutornet.tutor_net.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/permissions")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('user:assign_role')")
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermissionSummary>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.getAll()));
    }

    // Nhóm permission theo module, dùng cho trang quản lý phân quyền
    @GetMapping("/grouped")
    @PreAuthorize("hasAuthority('permission:read')")
    public ResponseEntity<ApiResponse<List<PermissionGroupResponse>>> getGrouped() {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.getGroupedPermissions()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionSummary>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PermissionSummary>> create(
            @Valid @RequestBody CreatePermissionRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(permissionService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PermissionSummary>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePermissionRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công",
                permissionService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        permissionService.delete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Xoá permission thành công"));
    }

    // Xem toàn bộ quyền hiện tại của 1 user (debug / audit)
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PermissionSummary>>> getUserPermissions(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(permissionService.getUserPermissions(userId)));
    }
}
