package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.RolePermissionRequest.*;
import com.tutornet.tutor_net.dto.response.PermissionGroupResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;

import java.util.List;
import java.util.Map;

public interface PermissionService {

    Map<String, List<PermissionSummary>> getAllGroupedByModule();

    List<PermissionSummary> getAll();

    List<PermissionGroupResponse> getGroupedPermissions();

    PermissionSummary getById(Long id);

    PermissionSummary create(CreatePermissionRequest request);

    PermissionSummary update(Long id, UpdatePermissionRequest request);

    void delete(Long id);

    List<PermissionSummary> getUserPermissions(Long userId);
}
