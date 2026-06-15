package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorInvitation;
import org.springframework.stereotype.Component;

@Component
public class AdminTutorInvitationMapper {

    public TutorInvitationResponse.AdminTutorInvitationTableResponse toAdminResponse(TutorInvitation invitation) {
        if (invitation == null) return null;

        ClassRequest cr = invitation.getClassRequest();

        // Trích xuất an toàn tránh NullPointerException
        String classCode = cr != null ? cr.getClassCode() : "N/A";
        String subjectName = (cr != null && cr.getSubject() != null) ? cr.getSubject().getName() : "N/A";

        return TutorInvitationResponse.AdminTutorInvitationTableResponse.builder()
                .id(invitation.getId())
                .classCode(classCode)
                .subjectName(subjectName)
                .proposedPrice(cr != null ? cr.getProposedPrice() : null)
                .studentName(cr != null ? cr.getContactName() : "N/A")
                .studentPhone(cr != null ? cr.getContactPhone() : "N/A")
                .tutorId(invitation.getTutor().getId())
                .tutorName(invitation.getTutor().getUser().getFullName())
                .message(invitation.getMessage())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                // Giả định bạn đã thêm trường cancelReason vào Entity TutorInvitation
                // .cancelReason(invitation.getCancelReason())
                .build();
    }
}