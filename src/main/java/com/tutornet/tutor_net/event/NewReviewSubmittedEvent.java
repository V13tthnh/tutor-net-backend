package com.tutornet.tutor_net.event;

public record NewReviewSubmittedEvent(
        Long tutorUserId,
        String tutorEmail,
        String reviewerName,
        Integer rating,
        Long contractId
) {}
