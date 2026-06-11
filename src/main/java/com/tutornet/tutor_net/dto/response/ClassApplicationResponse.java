package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.ApplicationStatus;
import java.time.LocalDateTime;

public record ClassApplicationResponse(
        Long id,
        Long requestId,
        Long tutorId,
        String tutorName,
        String tutorAvatar,
        String tutorUniversity,
        String tutorMajor,
        ApplicationStatus status,
        String message,
        LocalDateTime createdAt
) {}
