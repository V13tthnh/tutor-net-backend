package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

/**
 * Bắn sau khi transaction "chấp nhận lời mời" commit thành công.
 *
 * @param classRequestId  ID của ClassRequest vừa được sinh ra (MATCHED)
 * @param invitationId    ID của TutorInvitation vừa ACCEPTED
 * @param tutorName       Tên gia sư (để ghi log & nội dung email/notif)
 * @param studentName     Tên học viên
 * @param studentEmail    Email học viên (null nếu khách vãng lai không nhập)
 * @param studentUser     Tài khoản học viên (null nếu khách vãng lai)
 */
public record TutorAcceptedInvitationEvent(
        Long classRequestId,
        Long invitationId,
        String tutorName,
        String studentName,
        String studentEmail,
        User studentUser
) {}