package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.TeachingMode;

import java.math.BigDecimal;
import java.util.List;

public final class TutorSearchResponse {

    /** Card hiển thị trong danh sách tìm kiếm */
    public record TutorCardResponse(
            Long id,
            String fullName,
            String avatarUrl,
            GenderType gender,
            String headline,
            String bio,
            Integer experienceYears,
            BigDecimal ratingAvg,
            Integer ratingCount,
            TeachingMode teachingMode,
            List<SubjectInfo> subjects,
            List<String> provinces,          // tỉnh/thành phố dạy
            BigDecimal minHourlyRate         // giá thấp nhất trong các môn
    ) {}

    public record SubjectInfo(
            Long id,
            String name,
            BigDecimal hourlyRate
    ) {}

    /** Dữ liệu cho các bộ lọc — lấy từ data thực tế trong DB */
    public record FilterOptionsResponse(
            List<SubjectOption> subjects,
            List<String> provinces,
            List<GenderOption> genders,
            List<TeachingModeOption> teachingModes
    ) {}

    public record SubjectOption(Long id, String name) {}

    public record GenderOption(String value, String label) {}

    public record TeachingModeOption(String value, String label) {}
}