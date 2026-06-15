package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCancelInvitationRequest(
        @NotBlank(message = "Vui lòng nhập lý do hủy lời mời để lưu vết hệ thống")
        @Size(max = 500, message = "Lý do hủy không được vượt quá 500 ký tự")
        String cancelReason
) {}