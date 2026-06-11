package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.TutorRequest.*;
import com.tutornet.tutor_net.dto.response.TutorResponse.*;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.TutorStatus;
import com.tutornet.tutor_net.event.TutorSubmittedForReviewEvent;
import com.tutornet.tutor_net.exception.*;
import com.tutornet.tutor_net.mapper.TutorProfileMapper;
import com.tutornet.tutor_net.repository.*;
import com.tutornet.tutor_net.service.TutorProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TutorProfileServiceImpl implements TutorProfileService {
    private final TutorProfileRepository tutorProfileRepo;
    private final UserRepository userRepository;
    private final TutorSubjectRepository tutorSubjectRepo;
    private final TutorAvailabilityRepository availabilityRepo;
    private final TutorCertificateRepository certificateRepo;
    private final SubjectRepository subjectRepo;
    private final TutorProfileMapper mapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Tạo hồ sơ gia sư lần đầu (status = DRAFT).
     */
    @Override
    @Transactional
    public TutorProfileResponse createProfile(Long userId, TutorProfileRequest request) {
        if (tutorProfileRepo.existsByUserId(userId)) {
            throw BusinessException.conflict("Hồ sơ gia sư đã tồn tại cho tài khoản này");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        TutorProfile profile = new TutorProfile();
        profile.setUser(user);
        profile.setStatus(TutorStatus.DRAFT);

        mapper.updateEntity(profile, request);

        if (request.teachingProvince() != null && !request.teachingProvince().isBlank()) {
            TutorTeachingArea area = new TutorTeachingArea();
            area.setTutor(profile);
            area.setProvince(request.teachingProvince());
            area.setWard(request.teachingWard());
            profile.getTeachingAreas().add(area);
        }

        userRepository.save(user);
        tutorProfileRepo.save(profile);

        return mapper.toResponse(getOwnProfile(userId));
    }

    @Override
    @Transactional
    public TutorProfileResponse getMyProfile(Long userId) {
        return mapper.toResponse(getOwnProfile(userId));
    }

    /**
     * Cập nhật thông tin linh hoạt (Bio, Headline, Mức lương).
     * Cho phép sửa khi DRAFT, REJECTED, và cả APPROVED.
     */
    @Override
    public TutorProfileResponse updateProfile(Long userId, TutorProfileRequest request) {
        TutorProfile profile = getOwnProfile(userId);

        // Chặn chỉnh sửa nếu đang chờ duyệt hoặc bị đình chỉ
        assertFlexibleDataEditable(profile);

        // Lưu ý: Tại tầng Mapper (TutorProfileMapper), bạn nên cấu hình để ignore các trường
        // định danh (CCCD, Ảnh thẻ) nếu profile.getStatus() == APPROVED để bảo mật tuyệt đối.
        mapper.updateEntity(profile, request);

        userRepository.save(profile.getUser());
        tutorProfileRepo.save(profile);

        return mapper.toResponse(getOwnProfile(userId));
    }

    /**
     * Thêm môn dạy (Dữ liệu quan trọng). Chỉ được sửa khi DRAFT/REJECTED.
     */
    @Override
    public TutorSubjectResponse addSubject(Long userId, TutorSubjectRequest request) {
        TutorProfile profile = getOwnProfile(userId);
        assertCriticalDataEditable(profile);

        Optional<TutorSubject> existingOpt = tutorSubjectRepo
                .findByTutorIdAndSubjectId(profile.getId(), request.subjectId());

        if (existingOpt.isPresent()) {
            TutorSubject existing = existingOpt.get();
            existing.setProficiencyLevel(request.proficiencyLevel());
            existing.setHourlyRate(request.hourlyRate());
            return mapper.toSubjectResponse(tutorSubjectRepo.save(existing));
        }

        Subject subject = subjectRepo.findById(request.subjectId())
                .orElseThrow(() -> ResourceNotFoundException.of("Môn học", request.subjectId()));

        TutorSubject ts = new TutorSubject();
        ts.setTutor(profile);
        ts.setSubject(subject);
        ts.setProficiencyLevel(request.proficiencyLevel());
        ts.setHourlyRate(request.hourlyRate());

        return mapper.toSubjectResponse(tutorSubjectRepo.save(ts));
    }

    @Override
    public void removeSubject(Long userId, Long subjectId) {
        TutorProfile profile = getOwnProfile(userId);
        assertCriticalDataEditable(profile);

        if (!tutorSubjectRepo.existsByTutorIdAndSubjectId(profile.getId(), subjectId)) {
            throw ResourceNotFoundException.of("Môn học", "subjectId", subjectId);
        }
        tutorSubjectRepo.deleteByTutorIdAndSubjectId(profile.getId(), subjectId);
    }

    /**
     * Thay toàn bộ lịch rảnh (Dữ liệu linh hoạt).
     * Cho phép sửa cả khi APPROVED.
     */
    @Override
    public List<AvailabilityResponse> replaceAvailability(Long userId, List<AvailabilityRequest> requests) {
        TutorProfile profile = getOwnProfile(userId);
        assertNotSuspended(profile);

        // Xóa bản ghi dưới Database và ép Hibernate thực thi (flush) ngay lập tức
        availabilityRepo.deleteAllByTutorId(profile.getId());
        availabilityRepo.flush();

        // Xóa sạch danh sách trong bộ nhớ để chặn Hibernate tự động lưu ngược dữ liệu cũ (Cascade Re-insert)
        if (profile.getAvailability() != null) {
            profile.getAvailability().clear();
        }

        List<TutorAvailability> slots = requests.stream().map(r -> {
            if (!r.endTime().isAfter(r.startTime())) {
                throw BusinessException.validationFailed(
                        "Thời gian kết thúc phải sau thời gian bắt đầu (day_of_week=" + r.dayOfWeek() + ")"
                );
            }
            TutorAvailability slot = new TutorAvailability();
            slot.setTutor(profile);
            slot.setDayOfWeek(r.dayOfWeek());
            slot.setStartTime(r.startTime());
            slot.setEndTime(r.endTime());
            return slot;
        }).toList();

        return availabilityRepo.saveAll(slots).stream()
                .map(mapper::toAvailabilityResponse).toList();
    }

    /**
     * Thêm chứng chỉ (Dữ liệu quan trọng). Chỉ được sửa khi DRAFT/REJECTED.
     */
    @Override
    public CertificateResponse addCertificate(Long userId, String name, String fileUrl) {
        TutorProfile profile = getOwnProfile(userId);

        assertNotSuspended(profile);

        TutorCertificate cert = new TutorCertificate();
        cert.setTutor(profile);
        cert.setName(name);
        cert.setFileUrl(fileUrl);
        cert.setIsVerified(false);

        return mapper.toCertificateResponse(certificateRepo.save(cert));
    }

    @Override
    public void removeCertificate(Long userId, Long certificateId) {
        TutorProfile profile = getOwnProfile(userId);

        assertNotSuspended(profile);

        TutorCertificate cert = certificateRepo.findById(certificateId)
                .orElseThrow(() -> ResourceNotFoundException.of("Chứng chỉ", certificateId));

        if (!cert.getTutor().getId().equals(profile.getId())) {
            throw BusinessException.invalidState("Chứng chỉ này không thuộc hồ sơ của bạn");
        }
        certificateRepo.delete(cert);
    }

    /**
     * Nộp hồ sơ để duyệt (Dành cho lần nộp đầu tiên từ DRAFT)
     */
    @Override
    public TutorProfileResponse submitForReview(Long userId) {
        TutorProfile profile = getOwnProfile(userId);

        if (profile.getStatus() == TutorStatus.PENDING_REVIEW) {
            return mapper.toResponse(profile);
        }

        if (profile.getStatus() != TutorStatus.DRAFT && profile.getStatus() != TutorStatus.REJECTED) {
            throw BusinessException.invalidState(
                    "Chỉ có thể nộp hồ sơ ở trạng thái Đang soạn thảo (DRAFT) hoặc Bị từ chối (REJECTED). " +
                            "Trạng thái hiện tại: " + profile.getStatus()
            );
        }

        validateSubmittable(profile);

        profile.setStatus(TutorStatus.PENDING_REVIEW);

        profile.setRejectionReason(null);

        TutorProfile savedProfile = tutorProfileRepo.save(profile);

        eventPublisher.publishEvent(new TutorSubmittedForReviewEvent(
                savedProfile.getId(),
                savedProfile.getUser().getFullName()
        ));

        return mapper.toResponse(savedProfile);
    }

    /**
     * Nộp lại hồ sơ (Dành riêng cho trạng thái REJECTED hoặc cập nhật lớn)
     */
    @Override
    @Transactional
    public TutorProfileResponse resubmitProfile(Long tutorUserId, TutorProfileRequest request) {
        TutorProfile profile = getOwnProfile(tutorUserId);

        if (profile.getStatus() != TutorStatus.REJECTED && profile.getStatus() != TutorStatus.DRAFT) {
            throw BusinessException.invalidState("Chỉ có thể nộp lại hồ sơ khi đang ở trạng thái Nháp hoặc Bị từ chối.");
        }

        if (request != null) {
            mapper.updateEntity(profile, request);
        }

        validateSubmittable(profile);

        profile.setStatus(TutorStatus.PENDING_REVIEW);
        profile.setRejectionReason(null);

        TutorProfile savedProfile = tutorProfileRepo.save(profile);

        // gửi thông báo cho admin
        eventPublisher.publishEvent(new TutorSubmittedForReviewEvent(
                savedProfile.getId(),
                savedProfile.getUser().getFullName()
        ));

        return mapper.toResponse(savedProfile);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS & VALIDATORS
    // ─────────────────────────────────────────────────────────────────────────

    private TutorProfile getOwnProfile(Long userId) {
        return tutorProfileRepo.findByUserIdWithDetails(userId)
                .orElseThrow(() -> ResourceNotFoundException.of(
                        "Hồ sơ gia sư", "userId", userId));
    }

    /**
     * Xác thực quyền sửa Dữ liệu Quan Trọng (Bằng cấp, Chứng chỉ, Môn học).
     * Bị khóa hoàn toàn nếu đã APPROVED.
     */
    private void assertCriticalDataEditable(TutorProfile profile) {
        if (profile.getStatus() != TutorStatus.DRAFT
                && profile.getStatus() != TutorStatus.REJECTED) {
            throw BusinessException.invalidState(
                    "Không thể thêm/xóa Bằng cấp hoặc Môn học ở trạng thái " + profile.getStatus() +
                            ". Vui lòng liên hệ Admin hoặc tạo yêu cầu cập nhật hồ sơ."
            );
        }
    }

    /**
     * Xác thực quyền sửa Dữ liệu Linh Hoạt (Bio, Headline, Khu vực).
     * Cho phép sửa kể cả khi APPROVED. Chặn khi đang PENDING_REVIEW hoặc SUSPENDED.
     */
    private void assertFlexibleDataEditable(TutorProfile profile) {
        if (profile.getStatus() == TutorStatus.PENDING_REVIEW || profile.getStatus() == TutorStatus.SUSPENDED) {
            throw BusinessException.invalidState(
                    "Hồ sơ đang ở trạng thái " + profile.getStatus() + " nên không thể chỉnh sửa."
            );
        }
    }

    private void assertNotSuspended(TutorProfile profile) {
        if (profile.getStatus() == TutorStatus.SUSPENDED) {
            throw BusinessException.invalidState("Tài khoản đang bị đình chỉ");
        }
    }

    private void validateSubmittable(TutorProfile profile) {
        if (profile.getHeadline() == null || profile.getHeadline().isBlank()) {
            throw BusinessException.validationFailed("Vui lòng điền tiêu đề hồ sơ");
        }
        if (profile.getBio() == null || profile.getBio().isBlank()) {
            throw BusinessException.validationFailed("Vui lòng điền giới thiệu bản thân");
        }
        if (profile.getSubjects() == null || profile.getSubjects().isEmpty()) {
            throw BusinessException.validationFailed("Phải có ít nhất 1 môn dạy");
        }
        if (profile.getAvailability() == null || profile.getAvailability().isEmpty()) {
            throw BusinessException.validationFailed("Phải có ít nhất 1 khung lịch rảnh");
        }
        if (profile.getEducationLevel() == null) {
            throw BusinessException.validationFailed("Vui lòng cập nhật Trình độ học vấn hiện tại");
        }
    }
}