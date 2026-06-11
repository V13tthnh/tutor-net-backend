package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record InviteTutorRequest(
        @NotBlank(message = "Tên không được để trống") String fullName,
        @NotBlank(message = "Số điện thoại không được để trống") String phone,
        @NotBlank(message = "Email không được để trống") @Email String email,
        String message
) {}
