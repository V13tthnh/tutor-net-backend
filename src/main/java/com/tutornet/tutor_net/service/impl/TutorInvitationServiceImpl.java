package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.response.ContractPreviewResponse;
import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.event.TutorRespondedToInviteEvent;
import com.tutornet.tutor_net.exception.BadRequestException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.TutorInvitationMapper;
import com.tutornet.tutor_net.repository.*;
import com.tutornet.tutor_net.service.ContractService;
import com.tutornet.tutor_net.service.TutorInvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorInvitationServiceImpl implements TutorInvitationService {

    private final TutorInvitationRepository  invitationRepository;
    private final TutorProfileRepository     tutorProfileRepository;
    private final ClassRequestRepository classRequestRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final TutorInvitationMapper invitationMapper;
    private final ContractRepository contractRepository;
    private final ContractService contractService;

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

    /**
     * 2. XÁC NHẬN KÝ & NHẬN LỚP (Gộp chung DB + PDF)
     */
    @Override
    @Transactional
    public void acceptAndSignContract(Long invitationId, Long tutorUserId, String ipAddress) {

        TutorInvitation invitation = loadAndVerifyOwnership(invitationId, tutorUserId);
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Lời mời không hợp lệ để chấp nhận.");
        }

        ClassRequest classRequest = invitation.getClassRequest();

        // Trường hợp 2 gia sư được mời cùng lúc và cùng bấm nhận
        if (classRequest.getStatus() == ClassRequestStatus.MATCHED || classRequest.getStatus() == ClassRequestStatus.CANCELLED) {
            throw new BadRequestException("Rất tiếc, lớp học này đã có gia sư khác nhận hoặc đã bị hủy.");
        }

        // Cập nhật lời mời
        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);

        classRequest.setStatus(ClassRequestStatus.MATCHED);
        classRequest.setTargetTutor(invitation.getTutor()); // Gắn cứng gia sư này vào lớp
        classRequestRepository.save(classRequest);

        // Khởi tạo Contract vào DB
        BigDecimal hourlyRate = classRequest.getHourlyRate() != null ? classRequest.getHourlyRate() : classRequest.getProposedPrice();
        BigDecimal introFee = hourlyRate.multiply(BigDecimal.valueOf(16)).multiply(BigDecimal.valueOf(0.40));
        String contractNumber = "HD-" + LocalDate.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Contract contract = Contract.builder()
                .contractNumber(contractNumber)
                .classRequest(classRequest)
                .tutor(invitation.getTutor())
                .introductionFee(introFee)
                .status(ContractStatus.PENDING_SIGNATURE)
                .freeTrialCount(1)
                .isFeePaid(false)
                .effectiveDate(Instant.now())
                .build();
        contract = contractRepository.save(contract);

        // in PDF từ ContractService
        contractService.signContractAndGeneratePdf(contract.getId(), ipAddress, tutorUserId);
    }

    // ---------------------------------------------------------------
    // REJECT
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void rejectInvitation(Long invitationId, Long tutorUserId, String rejectionReason) {

        TutorInvitation invitation = loadAndVerifyOwnership(invitationId, tutorUserId);

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new BadRequestException("Không thể từ chối lời mời ở trạng thái " + invitation.getStatus());
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);

        ClassRequest cr = invitation.getClassRequest();
        User studentUser = cr.getUser(); // Có thể null nếu vãng lai

        eventPublisher.publishEvent(new TutorRespondedToInviteEvent(
                cr.getId(),
                cr.getContactName(),
                cr.getContactEmail(),
                studentUser,
                invitation.getTutor().getUser().getFullName(),
                false
        ));
    }


    /**
     * 1. LẤY DỮ LIỆU NHÁP HIỂN THỊ LÊN MODAL (PREVIEW)
     */
    @Override
    @Transactional(readOnly = true)
    public ContractPreviewResponse getContractPreview(Long invitationId, Long tutorUserId) {
        TutorInvitation invitation = loadAndVerifyOwnership(invitationId, tutorUserId);
        ClassRequest cr = invitation.getClassRequest();
        User tutorUser = invitation.getTutor().getUser();

        BigDecimal hourlyRate = cr.getHourlyRate() != null ? cr.getHourlyRate() : (cr.getProposedPrice() != null ? cr.getProposedPrice() : BigDecimal.ZERO);
        BigDecimal introFee = hourlyRate.multiply(BigDecimal.valueOf(16)).multiply(BigDecimal.valueOf(0.40));

        StringBuilder sb = new StringBuilder();
        if (cr.getSessionsPerWeek() != null) {
            sb.append(cr.getSessionsPerWeek()).append(" buổi / tuần");
        }
        if (cr.getDurationMinutes() != null) {
            sb.append(" – Mỗi buổi ").append(cr.getDurationMinutes()).append(" phút");
        }
        String detailedSchedule = sb.isEmpty() ? "Theo thỏa thuận" : sb.toString();

        boolean revealed = InvitationStatus.ACCEPTED.equals(invitation.getStatus());

        return new ContractPreviewResponse(
                tutorUser.getFullName(),
                tutorUser.getBirthYear() != null ? tutorUser.getBirthYear() : 2000,
                tutorUser.getPhone(),
                tutorUser.getEmail(),
                cr.getContactName(),
                revealed ? cr.getContactPhone() : TutorInvitationMapper.maskPhone(cr.getContactPhone()),
                revealed ? cr.getContactEmail() : TutorInvitationMapper.maskEmail(cr.getContactEmail()),
                "Địa chỉ chi tiết sẽ hiển thị sau khi ký nhận",
                cr.getSubject() != null ? cr.getSubject().getName() : "N/A",
                hourlyRate,
                cr.getSessionsPerWeek() + " buổi / tuần",
                introFee,
                cr.getClassCode(),
                cr.getGradeLevel()
        );
    }

    // Load lời mời, kiểm tra gia sư đang đăng nhập có phải chủ sở hữu không
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
}