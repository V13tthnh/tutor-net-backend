package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
import com.tutornet.tutor_net.dto.response.TutorResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.event.TutorInvitedEvent;
import com.tutornet.tutor_net.event.TutorRespondedToInviteEvent;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.TutorProfileMapper;
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.TutorInvitationRepository;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.service.TutorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final TutorProfileRepository    tutorProfileRepository;
    private final TutorInvitationRepository invitationRepository;
    private final ClassRequestRepository    classRequestRepository;
    private final TutorProfileMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void processTutorInvitation(Long tutorId, Long studentUserId, InviteTutorRequest request) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Gia sư không tồn tại"));

        if (studentUserId == null) {
            throw new BusinessException("Bạn cần đăng nhập để thực hiện chức năng này");
        }

        if (studentUserId.equals(tutor.getUser().getId())) {
            throw new BusinessException("Bạn không thể tự gửi lời mời dạy học cho chính mình");
        }

        // Lấy thông tin Lớp học từ Dropdown mà học viên chọn
        ClassRequest classRequest = classRequestRepository.findById(request.classRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu lớp học để liên kết"));

        if (classRequest.getUser() == null || !classRequest.getUser().getId().equals(studentUserId)) {
            throw new BusinessException("Bạn không có quyền sử dụng lớp học này để mời gia sư");
        }

        if (classRequest.getStatus() != ClassRequestStatus.APPROVED) {
            throw new BusinessException("Lớp học phải được phê duyệt trước khi mời gia sư dạy");
        }

        // logic chặn spam mời gia sư
        boolean isAlreadyPending = invitationRepository.existsByClassRequest_IdAndTutor_IdAndStatus(
                classRequest.getId(),
                tutor.getId(),
                InvitationStatus.PENDING
        );

        if (isAlreadyPending) {
            throw new BusinessException("Bạn đã gửi lời mời cho gia sư này và đang chờ phản hồi. Vui lòng không gửi lại.");
        }

        // khởi tạo lời mời với khoá ngoại
        TutorInvitation invitation = TutorInvitation.builder()
                .tutor(tutor)
                .classRequest(classRequest)
                .message(request.message())
                .status(InvitationStatus.PENDING)
                .build();

        TutorInvitation saved = invitationRepository.save(invitation);

        eventPublisher.publishEvent(new TutorInvitedEvent(
                saved.getId(),
                tutor.getUser(),
                tutor.getUser().getEmail(),
                tutor.getUser().getFullName(),
                classRequest.getContactName(),
                request.message()
        ));
    }

    @Override
    @Transactional
    public void acceptTutorInvitation(Long invitationId, Long tutorUserId) {
        TutorInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời này"));

        if (!invitation.getTutor().getUser().getId().equals(tutorUserId)) {
            throw new BusinessException("Bạn không có quyền thao tác trên thư mời này");
        }

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Lời mời này đã được xử lý hoặc đã hết hạn");
        }

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        // cập nhật trang thái lớp cũ
        ClassRequest classRequest = invitation.getClassRequest();
        classRequest.setStatus(ClassRequestStatus.MATCHED);
        classRequest.setTargetTutor(invitation.getTutor());
        classRequestRepository.save(classRequest);

        User studentUser = classRequest.getUser();

        eventPublisher.publishEvent(new TutorRespondedToInviteEvent(
                classRequest.getId(),
                classRequest.getContactName(),
                classRequest.getContactEmail(),
                studentUser,
                invitation.getTutor().getUser().getFullName(),
                true
        ));
    }

    @Override
    public TutorResponse.TutorProfileResponse getTutorById(Long tutorId) {
        TutorProfile profile = findWithDetailsOrThrow(tutorId);
        return mapper.toResponse(profile);
    }

    private TutorProfile findWithDetailsOrThrow(Long tutorId) {
        return tutorProfileRepository.findByIdWithDetails(tutorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Hồ sơ gia sư", tutorId));
    }
}