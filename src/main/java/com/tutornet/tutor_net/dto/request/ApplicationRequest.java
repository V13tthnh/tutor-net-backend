package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.ApplicationStatus;
import jakarta.validation.constraints.*;

public class ApplicationRequest {

    // Gia sư bấm ứng tuyển
    public record ApplyClassRequest(
            @NotNull Long requestId,
            @Size(max = 1000) String message,
            @Email String contactEmail // Thêm trường này để nhận email từ vãng lai
    ) {}

    // Học viên hoặc Admin duyệt hồ sơ ứng tuyển của gia sư
    public record UpdateApplicationStatusRequest(
            @NotNull(message = "Vui lòng chọn trạng thái phê duyệt")
            ApplicationStatus status
    ) {}

}
