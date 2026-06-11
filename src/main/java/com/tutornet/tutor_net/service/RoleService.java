package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.AssignUserRoleRequest;
import com.tutornet.tutor_net.dto.request.RolePermissionRequest.*;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import org.springframework.data.domain.Pageable;

public interface RoleService {

    PageResponse<RoleResponse> getRoles(String keyword, Boolean isSystem, Pageable pageable);

    RoleResponse getRoleById(Long id);

    RoleResponse createRole(CreateRoleRequest request);

    RoleResponse updateRole(Long id, UpdateRoleRequest request);

    void deleteRole(Long id);

    RoleResponse syncPermissions(Long roleId, SyncPermissionsRequest request);

    RoleResponse addPermission(Long roleId, TogglePermissionRequest request);

    RoleResponse removePermission(Long roleId, Long permissionId);

    PageResponse<UserSummaryResponse> getUsersByRole(Long roleId, Pageable pageable);

    void assignUserToRole(Long roleId, AssignUserRoleRequest request);

    void revokeUserFromRole(Long roleId, Long userId);

    void exportRoles();
}
