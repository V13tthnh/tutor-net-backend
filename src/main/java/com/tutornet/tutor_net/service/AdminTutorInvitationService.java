package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.AdminCancelInvitationRequest;
import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.enums.InvitationStatus;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface AdminTutorInvitationService {
    UserRoleResponse.PageResponse<TutorInvitationResponse.AdminTutorInvitationTableResponse> getAllInvitations(
            String keyword, InvitationStatus status, Instant startDate, Instant endDate, Pageable pageable);

    void forceCancelInvitation(Long id, AdminCancelInvitationRequest request, Long adminId);
}
