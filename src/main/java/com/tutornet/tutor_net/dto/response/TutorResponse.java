package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.EduLevel;
import com.tutornet.tutor_net.enums.ProficiencyLevel;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.enums.TutorStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.List;

public final class TutorResponse {
    public record TutorProfileResponse(
            Long id,
            Long userId,
            String fullName,
            String avatarUrl,

            String            province,
            String            ward,
            String            address,

            String            hometownProvince,
            String            hometownWard,
            String            hometownAddress,

            String headline,
            String bio,
            Integer experienceYears,
            EduLevel educationLevel,
            Boolean isAvailable,
            TeachingMode teachingMode,
            List<TeachingAreaResponse> teachingAreas,
            TutorStatus status,
            BigDecimal ratingAvg,
            Integer ratingCount,
            String occupation,
            Integer studentYear,
            String major,
            String university,
            Integer graduationYear,
            String achievements,
            String idCardFrontUrl,
            Instant termsAcceptedAt,
            List<TutorSubjectResponse> subjects,
            List<CertificateResponse> certificates,
            List<AvailabilityResponse> availability,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record TutorSubjectResponse(
            Long id,
            Long subjectId,
            String subjectName,
            ProficiencyLevel proficiencyLevel,
            BigDecimal hourlyRate
    ) {}

    public record CertificateResponse(
            Long id,
            String name,
            String fileUrl,
            Boolean isVerified
    ) {}

    public record AvailabilityResponse(
            Long id,
            int dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {}

    public record TeachingAreaResponse(
            Long id,
            String province,
            String ward
    ) {}

    // Dùng cho danh sách table
    public record TutorSummaryResponse(
            Long id,
            Long userId,
            String fullName,
            String avatarUrl,
            String email,
            String phone,
            TutorStatus status,
            EduLevel educationLevel,
            List<String> subjectNames,  // chỉ cần tên, không cần chi tiết
            Integer experienceYears,
            BigDecimal ratingAvg,
            Integer ratingCount,
            Instant createdAt,
            Instant updatedAt
    ) {}

    // Dùng cho stats card
    public record TutorStatsResponse(
            long total,
            long pendingReview,
            long approved,
            long rejected
    ) {}

    // danh sách lựa chọn lọc
    public record TutorFilterOptionsResponse(
            List<StatusOption> statuses,
            List<SubjectOption> subjects
    ) {
        public record StatusOption(String value, String label) {}
        public record SubjectOption(Long id, String name) {}
    }
}
