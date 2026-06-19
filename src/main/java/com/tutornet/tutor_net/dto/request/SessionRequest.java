package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.SessionStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public class SessionRequest {

    public record CreateSessionRequest(
            @NotNull(message = "Mã hợp đồng/lớp học không được để trống")
            Long contractId,

            @NotNull(message = "Thời gian học không được để trống")
            Instant scheduledAt,

            @Min(value = 15, message = "Thời lượng buổi học tối thiểu là 15 phút")
            Integer durationMinutes,

            String teachingMode,
            String meetingUrl,
            String locationDetail,

            @DecimalMin(value = "0.0", message = "Giá buổi học không được âm")
            BigDecimal price,

            String studentNotes
    ) {}

    public record UpdateSessionStatusRequest(
            @NotNull(message = "Trạng thái buổi học không được để trống")
            SessionStatus status,

            String tutorNotes
    ) {}
}