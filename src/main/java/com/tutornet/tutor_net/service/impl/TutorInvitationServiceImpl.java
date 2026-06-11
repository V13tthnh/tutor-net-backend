package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.event.TutorAcceptedInvitationEvent;
import com.tutornet.tutor_net.event.TutorRespondedToInviteEvent;
import com.tutornet.tutor_net.exception.BadRequestException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.TutorInvitationMapper;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.TutorInvitationRepository;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.service.TutorInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TutorInvitationServiceImpl implements TutorInvitationService {

    private final TutorInvitationRepository  invitationRepository;
    private final TutorProfileRepository     tutorProfileRepository;
    private final ClassRequestRepository classRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TutorInvitationMapper invitationMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<TutorInvitationResponse> getMyInvitations(Long tutorUserId,
                                                          InvitationStatus status,
                                                          Pageable pageable) {

        // Lấy TutorProfile theo userId của người đang đăng nhập
        TutorProfile tutorProfile = tutorProfileRepository
                .findByUserId(tutorUserId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy hồ sơ gia sư cho userId: " + tutorUserId));

        Page<TutorInvitation> invitations;

        if (status != null) {
            // Lọc theo status cụ thể
            invitations = invitationRepository
                    .findByTutorIdAndStatus(tutorProfile.getId(), status, pageable);
        } else {
            // Lấy tất cả, mới nhất lên đầu
            invitations = invitationRepository
                    .findByTutor_IdOrderByCreatedAtDesc(tutorProfile.getId(), pageable);
        }

        return invitations.map(invitationMapper::toResponse);
    }

    @Override
    @Transactional
    public void acceptInvitation(Long invitationId, Long tutorUserId) {

        // 1. Tải lời mời và xác thực quyền sở hữu
        TutorInvitation invitation = loadAndVerifyOwnership(invitationId, tutorUserId);

        // 2. Chỉ PENDING mới được chấp nhận
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException(
                    "Lời mời #" + invitationId + " đang ở trạng thái "
                            + invitation.getStatus() + ", không thể chấp nhận.");
        }

        // 3. Đổi trạng thái lời mời → ACCEPTED
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        // 4. Tự động sinh ClassRequest chính thức (status = MATCHED)
        ClassRequest classRequest = buildMatchedClassRequest(invitation);
        classRequestRepository.save(classRequest);

        // 5. Lấy User học viên (null nếu khách vãng lai)
        User studentUser = resolveStudentUser(invitation);

        // 6. Bắn event — AppEventListener sẽ tạo Contract DRAFT + gửi mail/notif
        eventPublisher.publishEvent(new TutorAcceptedInvitationEvent(
                classRequest.getId(),
                invitation.getId(),
                invitation.getTutor().getUser().getFullName(),
                invitation.getStudentName(),
                invitation.getStudentEmail(),
                studentUser
        ));
    }

    // ---------------------------------------------------------------
    // REJECT
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void rejectInvitation(Long invitationId, Long tutorUserId, String rejectionReason) {

        TutorInvitation invitation = loadAndVerifyOwnership(invitationId, tutorUserId);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException(
                    "Lời mời #" + invitationId + " đang ở trạng thái "
                            + invitation.getStatus() + ", không thể từ chối.");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        // Bắn event thông báo học viên (dùng lại TutorRespondedToInviteEvent có sẵn)
        User studentUser = resolveStudentUser(invitation);
        eventPublisher.publishEvent(new TutorRespondedToInviteEvent(
                null,                                           // Chưa có classRequestId khi từ chối
                invitation.getStudentName(),
                invitation.getStudentEmail(),
                studentUser,
                invitation.getTutor().getUser().getFullName(),
                false                                           // isAccepted = false
        ));
    }

    // ---------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------

    /**
     * Load lời mời, kiểm tra gia sư đang đăng nhập có phải chủ sở hữu không.
     */
    private TutorInvitation loadAndVerifyOwnership(Long invitationId, Long tutorUserId) {
        TutorInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy lời mời #" + invitationId));

        Long ownerUserId = invitation.getTutor().getUser().getId();
        if (!ownerUserId.equals(tutorUserId)) {
            throw new AccessDeniedException("Bạn không có quyền thao tác với lời mời này.");
        }
        return invitation;
    }

    /**
     * Chuyển dữ liệu TutorInvitation → ClassRequest mới với status MATCHED.
     * Đây là bản ghi "lớp học chính thức" dùng để liên kết với Contract.
     */
    private ClassRequest buildMatchedClassRequest(TutorInvitation invitation) {
        return ClassRequest.builder()
                // Học viên có tài khoản → liên kết User; khách vãng lai → null
                .user(resolveStudentUser(invitation))
                .contactName(invitation.getStudentName())
                .contactPhone(invitation.getStudentPhone())
                .contactEmail(invitation.getStudentEmail())
                // TutorInvitation không lưu subject → dùng subject từ TutorProfile nếu có,
                // hoặc cần bổ sung field subjectId vào TutorInvitation sau này.
                // Hiện tại để null và admin/hệ thống điền sau.
                .subject(null)
                .gradeLevel("N/A")                         // Sẽ cập nhật sau khi gia sư xác nhận
                .teachingMode(TeachingMode.ONLINE)         // Mặc định, gia sư có thể cập nhật
                .studentNotes(invitation.getMessage())
                .targetTutor(invitation.getTutor())        // Gia sư đã chấp nhận
                .status(ClassRequestStatus.MATCHED)        // Thẳng vào MATCHED
                .build();
    }

    /**
     * Lấy User học viên từ studentUserId (null nếu khách vãng lai).
     */
    private User resolveStudentUser(TutorInvitation invitation) {
        if (invitation.getStudentUserId() == null) return null;
        return userRepository.findById(invitation.getStudentUserId()).orElse(null);
    }
}