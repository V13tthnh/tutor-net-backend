package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record ClassRequestReviewedEvent(
        User recipient,
        Long classRequestId,
        String subjectName,
        boolean isApproved,
        String rejectionReason
) {}
