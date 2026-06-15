package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteTutorRequest(

        @NotNull(message = "Vui lòng chọn yêu cầu lớp học để mời gia sư")
        Long classRequestId,

        @Size(max = 500, message = "Lời nhắn gửi gia sư không được vượt quá 500 ký tự")
        String message

) {}
