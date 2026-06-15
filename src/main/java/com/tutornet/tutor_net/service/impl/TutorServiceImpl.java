package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
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
import com.tutornet.tutor_net.repository.ClassRequestRepository;
import com.tutornet.tutor_net.repository.TutorInvitationRepository;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.service.TutorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final TutorProfileRepository    tutorProfileRepository;
    private final UserRepository            userRepository;
    private final TutorInvitationRepository invitationRepository;
    private final ClassRequestRepository    classRequestRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public void processTutorInvitation(Long tutorId, Long studentUserId, InviteTutorRequest request) {
        TutorProfile tutor = tutorProfileRepository.findById(tutorId)
                .orElseThrow(() -> new ResourceNotFoundException("Gia sư không tồn tại"));

        // Lấy thông tin Lớp học từ Dropdown mà học viên chọn
        ClassRequest classRequest = classRequestRepository.findById(request.classRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy yêu cầu lớp học để liên kết"));

        // logic chặn spam mời gia sư
        boolean isAlreadyPending = invitationRepository.existsByClassRequest_IdAndTutor_IdAndStatus(
                classRequest.getId(),
                tutor.getId(),
                InvitationStatus.PENDING
        );

        if (isAlreadyPending) {
            throw new BusinessException("Bạn đã gửi lời mời cho gia sư này và đang chờ phản hồi. Vui lòng không gửi lại.");
        }

        // KHỞI TẠO LỜI MỜI MỚI CHỈ VỚI KHÓA NGOẠI
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
                classRequest.getContactName(), // Lấy tên từ classRequest
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

        // 🌟 KHÔNG TẠO LỚP MỚI NỮA, CẬP NHẬT TRẠNG THÁI LỚP CŨ
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
}