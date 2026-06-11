package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.TutorStatus;

public record TutorReviewedEvent(
        Long tutorProfileId,
        User tutorUser,
        TutorStatus newStatus,
        String rejectionReason   // null nếu APPROVED
) {
    public String tutorEmail()    { return tutorUser.getEmail(); }
    public String tutorFullName() { return tutorUser.getFullName(); }
}