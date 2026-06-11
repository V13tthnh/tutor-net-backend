package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.NotNull;

public final class AssignUserRoleRequest {

    @NotNull(message = "userId không được để trống")
    private Long userId;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
