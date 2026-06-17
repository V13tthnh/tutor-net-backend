package com.tutornet.tutor_net.event;

public record TutorApplicationAcceptedEvent(
        Long classRequestId,
        Long applicationId,
        Long tutorUserId,      // Để gửi In-app Notification
        String tutorEmail,     // Để gửi Email
        String tutorName,
        String studentName
) {}

