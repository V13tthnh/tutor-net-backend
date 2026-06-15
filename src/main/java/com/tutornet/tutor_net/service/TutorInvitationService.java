package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.response.ContractPreviewResponse;
import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.enums.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TutorInvitationService {
    Page<TutorInvitationResponse> getMyInvitations(Long tutorUserId,
                                                   InvitationStatus status,
                                                   Pageable pageable);

    /**
     * Gia sư chấp nhận lời mời.
     * Trong một transaction:
     *  1. Đổi TutorInvitation → ACCEPTED
     *  2. Sinh ClassRequest mới (status = MATCHED)
     *  3. Bắn TutorAcceptedInvitationEvent (AppEventListener xử lý tạo Contract + gửi mail)
     *
     * @param invitationId ID lời mời
     * @param tutorUserId  ID user của gia sư đang đăng nhập (để xác thực quyền sở hữu)
     */
    void acceptAndSignContract(Long invitationId, Long tutorUserId, String ipAddress);

    /**
     * Gia sư từ chối lời mời.
     *
     * @param invitationId    ID lời mời
     * @param tutorUserId     ID user của gia sư đang đăng nhập
     * @param rejectionReason Lý do từ chối (tuỳ chọn)
     */
    void rejectInvitation(Long invitationId, Long tutorUserId, String rejectionReason);

    ContractPreviewResponse getContractPreview(Long invitationId, Long tutorUserId);
}
