package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.TutorRequest.*;
import com.tutornet.tutor_net.dto.response.TutorResponse.*;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.TutorStatus;
import com.tutornet.tutor_net.event.TutorReviewedEvent;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.TutorProfileMapper;
import com.tutornet.tutor_net.repository.TutorProfileRepository;
import com.tutornet.tutor_net.repository.spec.TutorProfileSpecification;
import com.tutornet.tutor_net.service.AdminTutorService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminTutorServiceImpl implements AdminTutorService {

    private final TutorProfileRepository tutorProfileRepo;
    private final TutorProfileMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public UserRoleResponse.PageResponse<TutorSummaryResponse> list(
            String keyword,
            List<TutorStatus> statuses,
            List<Long> subjectIds,
            Pageable pageable) {

        Specification<TutorProfile> spec = Specification
                .where(TutorProfileSpecification.hasKeyword(keyword))
                .and(TutorProfileSpecification.hasAnyStatus(statuses))
                .and(TutorProfileSpecification.hasAnySubject(subjectIds));

        Page<TutorProfile> page = tutorProfileRepo.findAll(spec, pageable);

        List<TutorSummaryResponse> content = page.getContent().stream()
                .map(mapper::toSummary)
                .toList();

        return new UserRoleResponse.PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }

    // ── 2. Chi tiết hồ sơ — xem CV đầy đủ ─────────────────────────────────

    @Override
    public TutorProfileResponse getTutorById(Long tutorId) {
        TutorProfile profile = findWithDetailsOrThrow(tutorId);
        return mapper.toResponse(profile);
    }

    // ── 3. Stats ────────────────────────────────────────────────────────────

    @Override
    public TutorStatsResponse getStats() {
        return new TutorStatsResponse(
                tutorProfileRepo.count(),
                tutorProfileRepo.countByStatus(TutorStatus.PENDING_REVIEW),
                tutorProfileRepo.countByStatus(TutorStatus.APPROVED),
                tutorProfileRepo.countByStatus(TutorStatus.REJECTED)
        );
    }

    // ── 4. Duyệt / Từ chối ─────────────────────────────────────────────────

    @Override
    @Transactional
    public TutorProfileResponse reviewTutor(
            Long tutorId,
            ReviewTutorRequest request,
            Long adminId) {

        TutorProfile profile = findWithDetailsOrThrow(tutorId);

        // Chỉ được duyệt khi đang ở PENDING_REVIEW
        if (profile.getStatus() != TutorStatus.PENDING_REVIEW) {
            throw BusinessException.invalidState(
                    "Chỉ có thể duyệt hồ sơ ở trạng thái PENDING_REVIEW. " +
                            "Trạng thái hiện tại: " + profile.getStatus());
        }

        TutorStatus newStatus = request.status();

        // Validate: status review chỉ được là APPROVED hoặc REJECTED
        if (newStatus != TutorStatus.APPROVED && newStatus != TutorStatus.REJECTED) {
            throw BusinessException.validationFailed(
                    "Trạng thái duyệt chỉ được là APPROVED hoặc REJECTED");
        }

        if (newStatus == TutorStatus.REJECTED) {
            if (request.rejectionReason() == null || request.rejectionReason().isBlank()) {
                throw BusinessException.validationFailed(
                        "Vui lòng nhập lý do từ chối");
            }
            profile.setRejectionReason(request.rejectionReason().trim());
            profile.setVerifiedAt(null);
            profile.setVerifiedBy(null);
        }

        if (newStatus == TutorStatus.APPROVED) {
            profile.setRejectionReason(null);
            profile.setVerifiedAt(Instant.now());
            profile.setVerifiedBy(userRef(adminId));
        }

        profile.setStatus(newStatus);
        tutorProfileRepo.save(profile);

        User tutor = profile.getUser();

        // Gửi mail + notification
        eventPublisher.publishEvent(new TutorReviewedEvent(
                tutorId,
                tutor,
                newStatus,
                newStatus == TutorStatus.REJECTED ? profile.getRejectionReason() : null
        ));

        // Query lại để trả về đầy đủ associations
        return mapper.toResponse(findWithDetailsOrThrow(tutorId));
    }

    // ── Đình chỉ ──

    @Override
    @Transactional
    public void suspendTutor(Long tutorId, String reason) {
        TutorProfile profile = findWithDetailsOrThrow(tutorId);

        if (profile.getStatus() == TutorStatus.SUSPENDED) {
            throw BusinessException.invalidState("Tài khoản gia sư đã bị đình chỉ trước đó");
        }
        if (profile.getStatus() == TutorStatus.DRAFT
                || profile.getStatus() == TutorStatus.PENDING_REVIEW) {
            throw BusinessException.invalidState(
                    "Không thể đình chỉ hồ sơ ở trạng thái " + profile.getStatus());
        }

        profile.setStatus(TutorStatus.SUSPENDED);
        profile.setRejectionReason(reason != null ? reason.trim() : null);
        tutorProfileRepo.save(profile);
    }

    // ── Khôi phục sau đình chỉ ──

    @Override
    @Transactional
    public void unsuspendTutor(Long tutorId) {
        TutorProfile profile = findWithDetailsOrThrow(tutorId);

        if (profile.getStatus() != TutorStatus.SUSPENDED) {
            throw BusinessException.invalidState(
                    "Chỉ có thể khôi phục tài khoản đang ở trạng thái SUSPENDED. " +
                            "Trạng thái hiện tại: " + profile.getStatus());
        }

        // Khôi phục về APPROVED (đã từng được duyệt trước khi bị đình chỉ)
        profile.setStatus(TutorStatus.APPROVED);
        profile.setRejectionReason(null);
        tutorProfileRepo.save(profile);
    }

    @Override
    public TutorFilterOptionsResponse getFilterOptions() {
        List<TutorFilterOptionsResponse.StatusOption> statuses = Arrays.stream(TutorStatus.values())
                .map(s -> new TutorFilterOptionsResponse.StatusOption(s.name(), toStatusLabel(s)))
                .toList();

        List<TutorFilterOptionsResponse.SubjectOption> subjects = tutorProfileRepo
                .findDistinctSubjectsInProfiles()
                .stream()
                .map(s -> new TutorFilterOptionsResponse.SubjectOption(s.getId(), s.getName()))
                .toList();

        return new TutorFilterOptionsResponse(statuses, subjects);
    }

    private String toStatusLabel(TutorStatus status) {
        return switch (status) {
            case DRAFT          -> "Đang soạn thảo";
            case PENDING_REVIEW -> "Chờ duyệt";
            case APPROVED       -> "Đã duyệt";
            case REJECTED       -> "Từ chối";
            case SUSPENDED      -> "Đình chỉ";
        };
    }
    
    // ── Helpers ──

    private TutorProfile findWithDetailsOrThrow(Long tutorId) {
        return tutorProfileRepo.findByIdWithDetails(tutorId)
                .orElseThrow(() -> ResourceNotFoundException.of("Hồ sơ gia sư", tutorId));
    }

    /** Tạo proxy User chỉ có id để set verifiedBy, tránh query thêm */
    private User userRef(Long userId) {
        User u = new User();
        u.setId(userId);
        return u;
    }

}
