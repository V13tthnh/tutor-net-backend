package com.tutornet.tutor_net.dto.response;

import lombok.Builder;
import java.time.Instant;

@Builder
public record PublicReviewResponse(
        Long id,
        Integer rating,
        String comment,
        String reviewerName,
        Instant createdAt
) {}
