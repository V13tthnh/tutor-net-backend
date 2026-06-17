package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ClassRequestOwnResponse(
        Long id,
        String classCode,
        String subjectName,
        String gradeLevel,
        BigDecimal proposedPrice,
        BigDecimal hourlyRate,
        TeachingMode teachingMode,
        Integer sessionsPerWeek,
        ClassRequestStatus status,
        LocalDateTime createdAt,
        int applicantsCount
) {}