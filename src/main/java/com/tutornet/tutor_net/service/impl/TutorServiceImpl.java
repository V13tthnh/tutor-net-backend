package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
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

        TutorInvitation invitation = TutorInvitation.builder()
                .tutor(tutor)
                .studentUserId(studentUserId)       // null nếu khách vãng lai
                .studentName(request.fullName())
                .studentPhone(request.phone())
                .studentEmail(request.email())
                .message(request.message())
                .status(InvitationStatus.PENDING)
                .build();

        TutorInvitation saved = invitationRepository.save(invitation);

        eventPublisher.publishEvent(new TutorInvitedEvent(
                saved.getId(),
                tutor.getUser(),
                tutor.getUser().getEmail(),
                tutor.getUser().getFullName(),
                request.fullName(),
                request.message()
        ));
    }

    @Override
    @Transactional
    public void acceptTutorInvitation(Long invitationId, Long tutorUserId) {

        // 1. Tìm lời mời
        TutorInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời này"));

        // 2. Validate quyền: Chỉ gia sư nhận được lời mời mới có quyền chấp nhận
        if (!invitation.getTutor().getUser().getId().equals(tutorUserId)) {
            throw new BusinessException("Bạn không có quyền thao tác trên thư mời này");
        }

        // 3. Validate trạng thái: Chỉ xử lý khi còn đang PENDING
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BusinessException("Lời mời này đã được xử lý hoặc đã hết hạn");
        }

        // 4. Cập nhật trạng thái thư mời → ACCEPTED
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        // 5. Lấy User học viên (null nếu khách vãng lai)
        User studentUser = invitation.getStudentUserId() != null
                ? userRepository.findById(invitation.getStudentUserId()).orElse(null)
                : null;

        // 6. Tự động sinh ClassRequest chính thức (status = MATCHED)
        //    Map đúng tên field theo entity ClassRequest thực tế
        ClassRequest classRequest = ClassRequest.builder()
                .targetTutor(invitation.getTutor())
                .user(studentUser)
                .contactName(invitation.getStudentName())           // ← đúng field
                .contactPhone(invitation.getStudentPhone())         // ← đúng field
                .contactEmail(invitation.getStudentEmail())         // ← đúng field
                .studentNotes("Lớp được tạo từ lời mời trực tiếp. Lời nhắn: "
                        + invitation.getMessage())                  // ← đúng field
                .teachingMode(TeachingMode.ONLINE)                  // not-null, mặc định
                .gradeLevel("N/A")                                  // not-null, cập nhật sau
                .status(ClassRequestStatus.MATCHED)
                .build();

        ClassRequest savedRequest = classRequestRepository.save(classRequest);

        // 7. Bắn Event → AppEventListener tạo Contract DRAFT + gửi thông báo
        //    Đúng thứ tự tham số của TutorRespondedToInviteEvent record
        eventPublisher.publishEvent(new TutorRespondedToInviteEvent(
                savedRequest.getId(),                           // classRequestId
                invitation.getStudentName(),                    // studentName
                invitation.getStudentEmail(),                   // studentEmail (null nếu vãng lai)
                studentUser,                                    // studentUser  (null nếu vãng lai)
                invitation.getTutor().getUser().getFullName(),  // tutorName
                true                                            // isAccepted
        ));
    }
}