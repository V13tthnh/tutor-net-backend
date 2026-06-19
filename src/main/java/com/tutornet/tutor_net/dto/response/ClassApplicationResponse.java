package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.ApplicationStatus;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ClassApplicationResponse(
        Long id,
        Long classRequestId,

        Long tutorId,
        String tutorName,
        String tutorAvatarUrl,
        String university,
        String major,

        String headline,
        Integer experienceYears,

        ApplicationStatus status,
        String message,
        Instant appliedAt
) {}