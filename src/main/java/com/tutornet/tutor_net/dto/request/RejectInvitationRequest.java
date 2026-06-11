package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RejectInvitationRequest {

    @Size(max = 500, message = "Lý do từ chối không được vượt quá 500 ký tự")
    private String rejectionReason;
}