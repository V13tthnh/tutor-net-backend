package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.enums.TutorStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TutorSearchRepository extends JpaRepository<TutorProfile, Long>,
        JpaSpecificationExecutor<TutorProfile> {

    @Query("""
        SELECT DISTINCT tp FROM TutorProfile tp
        JOIN FETCH tp.user
        JOIN FETCH tp.subjects ts
        JOIN FETCH ts.subject
        LEFT JOIN FETCH tp.teachingAreas
        LEFT JOIN FETCH tp.certificates
        WHERE tp.status = TutorStatus.APPROVED
    """)
    List<TutorProfile> findAllApprovedWithDetails();

    @Query("""
    SELECT tp.id FROM TutorProfile tp
    WHERE tp.status = TutorStatus.APPROVED
    """)
    Page<Long> findApprovedIds(Specification<TutorProfile> spec, Pageable pageable);

    @EntityGraph(attributePaths = {
            "user",
            "subjects",
            "subjects.subject",
            "teachingAreas"
    })
    @Override
    Page<TutorProfile> findAll(Specification<TutorProfile> spec, Pageable pageable);

    /**
     * Lấy danh sách tỉnh/thành phố distinct từ các hồ sơ đã duyệt
     * → dùng cho filter options, không gán cứng
     */
    @Query("""
        SELECT DISTINCT ta.province
        FROM TutorTeachingArea ta
        WHERE ta.tutor.status = :status
        ORDER BY ta.province ASC
        """)
    List<String> findDistinctProvincesByStatus(@Param("status") TutorStatus status);

    /**
     * Lấy danh sách môn học distinct từ các hồ sơ đã duyệt
     */
    @Query("""
        SELECT DISTINCT ts.subject.id, ts.subject.name
        FROM TutorSubject ts
        WHERE ts.tutor.status = :status
          AND ts.subject.isActive = true
        ORDER BY ts.subject.name ASC
        """)
    List<Object[]> findDistinctSubjectsByStatus(@Param("status") TutorStatus status);

    /**
     * Lấy teaching modes distinct từ các hồ sơ đã duyệt
     * Không thể dùng JPQL cho Postgres array → dùng native query
     */
    @Query(value = """
    SELECT DISTINCT teaching_mode::TEXT
    FROM tutor_profiles
    WHERE status = 'APPROVED'
    """, nativeQuery = true)
    List<String> findDistinctTeachingModes();

    /**
     * Lấy gender distinct từ user của các hồ sơ đã duyệt
     */
    @Query("""
        SELECT DISTINCT u.gender
        FROM TutorProfile tp
        JOIN tp.user u
        WHERE tp.status = :status
        """)
    List<String> findDistinctGendersByStatus(@Param("status") TutorStatus status);
}