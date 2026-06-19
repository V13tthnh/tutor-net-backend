package com.tutornet.tutor_net.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminReviewResponse (
    Long id,
    Long contractId,
    String contractNumber,
    Long tutorId,
    String tutorFullName,
    String tutorEmail,
    String reviewerName,
    Integer rating,
    String comment,
    Boolean isPublic,
    Instant createdAt
){}
