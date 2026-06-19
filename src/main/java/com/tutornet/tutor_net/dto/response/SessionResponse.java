package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.SessionStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
import java.math.BigDecimal;
import java.time.Instant;

public record SessionResponse(
        Long id,
        Long contractId,
        String contractNumber,
        Long subjectId,
        String subjectName,
        Instant scheduledAt,
        Integer durationMinutes,
        TeachingMode teachingMode,
        String meetingUrl,
        String locationDetail,
        SessionStatus status,
        BigDecimal price,
        String currency,
        String tutorNotes,
        String studentNotes
) {}
