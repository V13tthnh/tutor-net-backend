package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.TutorSearchRequest.SearchFilter;
import com.tutornet.tutor_net.dto.response.TutorSearchResponse.*;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.enums.TutorStatus;
import com.tutornet.tutor_net.repository.TutorSearchRepository;
import com.tutornet.tutor_net.repository.spec.TutorSearchSpecification;
import com.tutornet.tutor_net.service.TutorSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TutorSearchServiceImpl implements TutorSearchService {

    private final TutorSearchRepository tutorSearchRepository;

    // ── Danh sách tìm kiếm ──────────────────────────────────────────────────

    @Override
    public UserRoleResponse.PageResponse<TutorCardResponse> search(
            SearchFilter filter, Pageable pageable) {

        Specification<TutorProfile> spec = Specification
                .where(TutorSearchSpecification.isApproved())
                .and(TutorSearchSpecification.hasNameKeyword(
                        filter.keyword()))
                .and(TutorSearchSpecification.hasAnySubjectId(
                        filter.subjectIds()))
                .and(TutorSearchSpecification.hasAnyProvince(
                        filter.provinces()))
                .and(TutorSearchSpecification.hasAnyGender(
                        filter.genders()))
                .and(TutorSearchSpecification.hasAnyTeachingMode(
                        filter.teachingModes()));

        Page<TutorProfile> page = tutorSearchRepository.findAll(spec, pageable);

        List<TutorCardResponse> content = page.getContent()
                .stream()
                .map(this::toCard)
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

    // ── Filter options — lấy từ data thực tế ────────────────────────────────

    @Override
    public FilterOptionsResponse getFilterOptions() {
        // Môn học
        List<SubjectOption> subjects = tutorSearchRepository
                .findDistinctSubjectsByStatus(TutorStatus.APPROVED)
                .stream()
                .map(row -> new SubjectOption(
                        ((Number) row[0]).longValue(),
                        (String) row[1]
                ))
                .toList();

        // Tỉnh/thành phố
        List<String> provinces = tutorSearchRepository
                .findDistinctProvincesByStatus(TutorStatus.APPROVED);

        // Giới tính — chỉ hiển thị gender có trong data thực tế
        List<GenderOption> genders = tutorSearchRepository
                .findDistinctGendersByStatus(TutorStatus.APPROVED)
                .stream()
                .map(g -> new GenderOption(g, toGenderLabel(g)))
                .toList();

        // Hình thức dạy — UNNEST từ Postgres array
        List<TeachingModeOption> teachingModes = tutorSearchRepository
                .findDistinctTeachingModes()
                .stream()
                .map(m -> new TeachingModeOption(m, toTeachingModeLabel(m)))
                .toList();

        return new FilterOptionsResponse(subjects, provinces, genders, teachingModes);
    }

    // ── Mapper ───────────────────────────────────────────────────────────────

    private TutorCardResponse toCard(TutorProfile p) {
        List<SubjectInfo> subjects = p.getSubjects().stream()
                .map(ts -> new SubjectInfo(
                        ts.getSubject().getId(),
                        ts.getSubject().getName(),
                        ts.getHourlyRate()
                ))
                .toList();

        BigDecimal minRate = subjects.stream()
                .map(SubjectInfo::hourlyRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        List<String> provinces = p.getTeachingAreas().stream()
                .map(ta -> ta.getProvince())
                .distinct()
                .sorted()
                .toList();

        return new TutorCardResponse(
                p.getId(),
                p.getUser().getFullName(),
                p.getUser().getAvatarUrl(),
                p.getUser().getGender(),
                p.getHeadline(),
                p.getBio(),
                p.getExperienceYears(),
                p.getRatingAvg(),
                p.getRatingCount(),
                p.getTeachingMode(),
                subjects,
                provinces,
                minRate
        );
    }

    private String toGenderLabel(String value) {
        try {
            return switch (GenderType.valueOf(value.toUpperCase())) {
                case MALE   -> "Nam";
                case FEMALE -> "Nữ";
                case OTHER  -> "Khác";
            };
        } catch (IllegalArgumentException e) {
            return value;
        }
    }

    private String toTeachingModeLabel(String value) {
        try {
            return switch (TeachingMode.valueOf(value.toUpperCase())) {
                case ONLINE  -> "Online";
                case OFFLINE -> "Tại nhà / Trung tâm";
                case HYBRID  -> "Linh hoạt";
            };
        } catch (IllegalArgumentException e) {
            return value;
        }
    }
}
