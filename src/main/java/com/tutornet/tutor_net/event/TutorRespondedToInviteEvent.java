package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record TutorRespondedToInviteEvent(
        Long classRequestId,
        String studentName,
        String studentEmail, // Có thể null nếu là khách vãng lai không nhập email
        User studentUser,    // Có thể null nếu là khách vãng lai
        String tutorName,
        boolean isAccepted   // TRUE: Gia sư đồng ý, FALSE: Gia sư từ chối
) {}
