package com.tutornet.tutor_net.event;

public record TutorSubmittedForReviewEvent(
        Long tutorProfileId,
        String tutorFullName
) {}
