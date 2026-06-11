package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.request.TutorRequest.*;
import com.tutornet.tutor_net.dto.response.TutorResponse.*;
import com.tutornet.tutor_net.entity.*;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.enums.TutorStatus;
import com.tutornet.tutor_net.util.AddressUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TutorProfileMapper {

    // ── TutorProfile ────────────────────────────────────────────────────────

    public TutorProfile toEntity(TutorProfileRequest request) {
        TutorProfile profile = new TutorProfile();
        // Khi tạo mới (DRAFT), cho phép map toàn bộ dữ liệu
        applyFlexibleFields(profile, request);
        applyCriticalFields(profile, request);
        return profile;
    }

    public void updateEntity(TutorProfile profile, TutorProfileRequest request) {
        // Luôn cho phép cập nhật các trường linh hoạt (Headline, Bio...)
        applyFlexibleFields(profile, request);

        // CHỐT CHẶN BẢO MẬT: Chỉ cập nhật các trường định danh (Ảnh, Giấy tờ, Trường học)
        // nếu hồ sơ CHƯA được duyệt (DRAFT hoặc REJECTED).
        // Nếu đã APPROVED, ngầm bỏ qua để tránh ghi đè bằng Postman/Hack.
        if (profile.getStatus() != TutorStatus.APPROVED) {
            applyCriticalFields(profile, request);
        }
    }

    public TutorProfileResponse toResponse(TutorProfile profile) {
        User user = profile.getUser();

        AddressUtils.Parts currentAddr = AddressUtils.parse(user.getCurrentAddress());
        AddressUtils.Parts hometownAddr = AddressUtils.parse(user.getHometownAddress());

        return new TutorProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getAvatarUrl(),

                currentAddr.province(),
                currentAddr.ward(),
                currentAddr.address(),

                hometownAddr.province(),
                hometownAddr.ward(),
                hometownAddr.address(),

                profile.getHeadline(),
                profile.getBio(),
                profile.getExperienceYears(),
                profile.getEducationLevel(),
                profile.getIsAvailable(),
                profile.getTeachingMode(),
                toTeachingAreaResponseList(profile.getTeachingAreas()),
                profile.getStatus(),
                profile.getRatingAvg(),
                profile.getRatingCount(),
                profile.getOccupation(),
                profile.getStudentYear(),
                profile.getMajor(),
                profile.getUniversity(),
                profile.getGraduationYear(),
                profile.getAchievements(),
                profile.getIdCardFrontUrl(),
                profile.getTermsAcceptedAt(),
                toSubjectResponseList(profile.getSubjects()),
                toCertificateResponseList(profile.getCertificates()),
                toAvailabilityResponseList(profile.getAvailability()),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }

    // ── TutorSubject ────────────────────────────────────────────────────────

    public TutorSubjectResponse toSubjectResponse(TutorSubject ts) {
        return new TutorSubjectResponse(
                ts.getId(),
                ts.getSubject().getId(),
                ts.getSubject().getName(),
                ts.getProficiencyLevel(),
                ts.getHourlyRate()
        );
    }

    public List<TutorSubjectResponse> toSubjectResponseList(Set<TutorSubject> subjects) {
        if (subjects == null) return Collections.emptyList();
        return subjects.stream()
                .map(this::toSubjectResponse)
                .collect(Collectors.toList());
    }

    // ── TutorCertificate ────────────────────────────────────────────────────

    public CertificateResponse toCertificateResponse(TutorCertificate cert) {
        return new CertificateResponse(
                cert.getId(),
                cert.getName(),
                cert.getFileUrl(),
                cert.getIsVerified()
        );
    }

    public List<CertificateResponse> toCertificateResponseList(Set<TutorCertificate> certs) {
        if (certs == null) return Collections.emptyList();
        return certs.stream()
                .map(this::toCertificateResponse)
                .collect(Collectors.toList());
    }

    // ── TutorAvailability ───────────────────────────────────────────────────

    public AvailabilityResponse toAvailabilityResponse(TutorAvailability slot) {
        return new AvailabilityResponse(
                slot.getId(),
                slot.getDayOfWeek(),
                slot.getStartTime(),
                slot.getEndTime()
        );
    }

    public List<AvailabilityResponse> toAvailabilityResponseList(Set<TutorAvailability> slots) {
        if (slots == null) return Collections.emptyList();
        return slots.stream()
                .map(this::toAvailabilityResponse)
                .collect(Collectors.toList());
    }

    // ── TutorTeachingArea ────────────────────────────────

    public TeachingAreaResponse toTeachingAreaResponse(TutorTeachingArea area) {
        return new TeachingAreaResponse(
                area.getId(),
                area.getProvince(),
                area.getWard()
        );
    }

    public List<TeachingAreaResponse> toTeachingAreaResponseList(Set<TutorTeachingArea> areas) {
        if (areas == null) return Collections.emptyList();
        return areas.stream()
                .map(this::toTeachingAreaResponse)
                .collect(Collectors.toList());
    }

    // ── Private helper ──────────────────────────────────────────────────────

    /**
     * Dữ liệu linh hoạt: Tiêu đề, Giới thiệu, Kinh nghiệm, Trạng thái rảnh, Hình thức dạy, Công việc, Thành tích.
     * Cập nhật an toàn bất kể trạng thái hồ sơ (trừ khi bị đình chỉ).
     */
    private void applyFlexibleFields(TutorProfile profile, TutorProfileRequest request) {
        if (request.headline() != null) profile.setHeadline(request.headline());
        if (request.bio() != null) profile.setBio(request.bio());
        if (request.experienceYears() != null) profile.setExperienceYears(request.experienceYears());
        if (request.isAvailable() != null) profile.setIsAvailable(request.isAvailable());
        if (request.teachingMode() != null) profile.setTeachingMode(request.teachingMode());
        if (request.occupation() != null) profile.setOccupation(request.occupation());
        if (request.achievements() != null) profile.setAchievements(request.achievements());
    }

    /**
     * Dữ liệu định danh/quan trọng: Ảnh thẻ, CCCD, Trường học, Chuyên ngành, Năm tốt nghiệp.
     * Không được phép ghi đè tự động nếu hồ sơ đã APPROVED.
     */
    private void applyCriticalFields(TutorProfile profile, TutorProfileRequest request) {
        if (request.educationLevel() != null) profile.setEducationLevel(request.educationLevel());
        if (request.studentYear() != null) profile.setStudentYear(request.studentYear());
        if (request.major() != null) profile.setMajor(request.major());
        if (request.university() != null) profile.setUniversity(request.university());
        if (request.graduationYear() != null) profile.setGraduationYear(request.graduationYear());
        if (request.idCardFrontUrl() != null) profile.setIdCardFrontUrl(request.idCardFrontUrl());
        if (request.avatarUrl() != null) {
            if (profile.getUser() != null) {
                profile.getUser().setAvatarUrl(request.avatarUrl());
            }
        }
    }

    public TutorSummaryResponse toSummary(TutorProfile profile) {
        User user = profile.getUser();

        List<String> subjectNames = profile.getSubjects() == null
                ? Collections.emptyList()
                : profile.getSubjects().stream()
                .map(ts -> ts.getSubject().getName())
                .collect(Collectors.toList());

        return new TutorSummaryResponse(
                profile.getId(),
                user.getId(),
                user.getFullName(),
                user.getAvatarUrl(),
                user.getEmail(),
                user.getPhone(),
                profile.getStatus(),
                profile.getEducationLevel(),
                subjectNames,
                profile.getExperienceYears(),
                profile.getRatingAvg(),
                profile.getRatingCount(),
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}