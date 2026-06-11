package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.InvitationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class TutorInvitationResponse {

    private Long id;

    // Thông tin học viên
    private Long studentUserId;      // null nếu khách vãng lai
    private String studentName;
    private String studentPhone;
    private String studentEmail;

    // Nội dung lời mời
    private String message;

    // Trạng thái
    private InvitationStatus status;

    private Instant createdAt;
}