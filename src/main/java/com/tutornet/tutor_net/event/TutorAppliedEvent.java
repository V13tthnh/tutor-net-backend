package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record TutorAppliedEvent(
        Long classRequestId,
        String studentName,
        String studentEmail,
        User studentUser,
        String tutorName
) {}