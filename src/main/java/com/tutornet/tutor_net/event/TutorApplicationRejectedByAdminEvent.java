package com.tutornet.tutor_net.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TutorApplicationRejectedByAdminEvent {
    private final Long applicationId;
    private final Long tutorUserId;
    private final String tutorEmail;
    private final String tutorFullName;
    private final String classContactName; // Tên học viên để gia sư biết lớp nào
}
