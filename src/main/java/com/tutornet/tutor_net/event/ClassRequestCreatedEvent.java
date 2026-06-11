package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record ClassRequestCreatedEvent(
        Long classRequestId,
        String studentName,
        String studentEmail, // Có thể null nếu khách vãng lai không nhập
        String subjectName,
        User targetTutorUser // Có null nếu đăng công khai, có giá trị nếu mời đích danh
) {}