package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record TutorInvitedEvent(
        Long invitationId,
        User tutorUser,          // ← đổi từ Long tutorUserId sang User object
        String tutorEmail,
        String tutorName,
        String studentName,
        String message
) {}