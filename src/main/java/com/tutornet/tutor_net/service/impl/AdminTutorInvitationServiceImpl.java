package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.AdminCancelInvitationRequest;
import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.AdminTutorInvitationMapper;
import com.tutornet.tutor_net.repository.TutorInvitationRepository;
import com.tutornet.tutor_net.repository.spec.TutorInvitationSpecification;
import com.tutornet.tutor_net.service.AdminTutorInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTutorInvitationServiceImpl implements AdminTutorInvitationService {

    private final TutorInvitationRepository invitationRepo;
    private final AdminTutorInvitationMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public UserRoleResponse.PageResponse<TutorInvitationResponse.AdminTutorInvitationTableResponse> getAllInvitations(
            String keyword, InvitationStatus status, Instant startDate, Instant endDate, Pageable pageable) {

        Specification<TutorInvitation> spec = TutorInvitationSpecification.filterForAdmin(keyword, status, startDate, endDate);

        Page<TutorInvitation> pageData = invitationRepo.findAll(spec, pageable);

        List<TutorInvitationResponse.AdminTutorInvitationTableResponse> content = pageData.getContent().stream()
                .map(mapper::toAdminResponse)
                .collect(Collectors.toList());

        return new UserRoleResponse.PageResponse<>(
                content,
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages(),
                pageData.isLast()
        );
    }

    @Override
    @Transactional
    public void forceCancelInvitation(Long id, AdminCancelInvitationRequest request, Long adminId) {
        TutorInvitation invitation = invitationRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lời mời với ID: " + id));

        // Chỉ cho phép hủy nếu lời mời đang ở trạng thái PENDING hoặc ACCEPTED
        if (invitation.getStatus() == InvitationStatus.CANCELED_BY_ADMIN || invitation.getStatus() == InvitationStatus.REJECTED) {
            throw new BusinessException("Lời mời này đã bị từ chối hoặc hủy bỏ từ trước.");
        }

        // Cập nhật trạng thái
        invitation.setStatus(InvitationStatus.CANCELED_BY_ADMIN);

        // Gợi ý: Bạn nên thêm cột 'cancel_reason' vào bảng tutor_invitations để lưu lý do
        // invitation.setCancelReason("Hủy bởi Admin ID " + adminId + ". Lý do: " + request.cancelReason());

        invitationRepo.save(invitation);

        log.info("Admin {} đã hủy ép buộc lời mời ID {}. Lý do: {}", adminId, id, request.cancelReason());

        // (Tùy chọn) Bắn Event gửi Mail thông báo cho Gia sư và Phụ huynh báo rằng Nền tảng đã thu hồi lời mời này
    }
}