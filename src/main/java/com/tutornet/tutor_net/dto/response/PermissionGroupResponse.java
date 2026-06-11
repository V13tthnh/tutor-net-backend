package com.tutornet.tutor_net.dto.response;

import java.util.List;

public record PermissionGroupResponse(
        String module,
        List<UserRoleResponse.PermissionSummary> permissions
) {}