package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record ClassRequestNotificationEvent(
        Long classRequestId,
        String subjectName,
        User targetTutorUser // Có giá trị nếu mời đích danh, null nếu đăng công khai
) {}