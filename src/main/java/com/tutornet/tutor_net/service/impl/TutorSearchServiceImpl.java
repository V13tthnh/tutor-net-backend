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

import jakarta.persistence.EntityManager;
import com.tutornet.tutor_net.util.SecuritySandboxHelper;
import org.springframework.web.bind.annotation.CrossOrigin;

@Service
@RequiredArgsConstructor
public class TutorSearchServiceImpl implements TutorSearchService {

    private final TutorSearchRepository tutorSearchRepository;
    private final EntityManager entityManager;

    // ── Danh sách tìm kiếm ──────────────────────────────────────────────────

    @Override
    public UserRoleResponse.PageResponse<TutorCardResponse> search(
            SearchFilter filter, Pageable pageable) {

        // --- SECURITY SANDBOX: UNION SQL INJECTION ---
        if (SecuritySandboxHelper.isVulnerable("union_sqli") && filter.keyword() != null && filter.keyword().contains("'")) {
            try {
                // Giả lập lổ hổng bằng cách ghép chuỗi trực tiếp
                String vulnerableQuery = "SELECT u.email, u.password_hash FROM users u WHERE u.full_name = '" + filter.keyword() + "'";
                List<Object[]> results = entityManager.createNativeQuery(vulnerableQuery).getResultList();
                
                // Biến đổi dữ liệu bị rò rỉ thành TutorCard để render lên giao diện
                List<TutorCardResponse> leakedData = results.stream().map(row -> new TutorCardResponse(
                        999L, // Fake ID
                        row[0].toString(), // Tên hiển thị Email
                        "https://api.dicebear.com/7.x/bottts/svg?seed=hacked",
                        GenderType.OTHER,
                        "LEAKED HASH: " + row[1].toString(), // Headline chứa Hash
                        "Dữ liệu mật khẩu bị lộ do lỗi SQL Injection!",
                        0, BigDecimal.ZERO, 0, TeachingMode.ONLINE,
                        java.util.Collections.emptyList(),
                        java.util.Collections.emptyList(),
                        java.math.BigDecimal.ZERO
                )).toList();
                
                return new UserRoleResponse.PageResponse<>(
                        leakedData,
                        0, 10, leakedData.size(), 1, true
                );
            } catch (Exception e) {
                System.out.println("Sandbox SQLi Exception: " + e.getMessage());
            }
        }
        // --- END SANDBOX ---

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
