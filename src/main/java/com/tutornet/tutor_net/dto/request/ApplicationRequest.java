package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.ApplicationStatus;
import jakarta.validation.constraints.*;

public class ApplicationRequest {

    // gia sư bấm ứng tuyển
    public record ApplyClassRequest(
            @Size(max = 500, message = "Lời nhắn giới thiệu không được vượt quá 500 ký tự")
            String message
    ) {}

    // học viên hoặc Admin duyệt hồ sơ ứng tuyển của gia sư
    public record UpdateApplicationStatusRequest(
            @NotNull(message = "Vui lòng chọn trạng thái phê duyệt")
            ApplicationStatus status
    ) {}
}
