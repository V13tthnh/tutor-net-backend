package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Subject;
import com.tutornet.tutor_net.entity.TutorProfile;
import com.tutornet.tutor_net.enums.TutorStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TutorProfileRepository extends JpaRepository<TutorProfile, Long>,
        JpaSpecificationExecutor<TutorProfile> {

    @Query("""
        SELECT DISTINCT tp FROM TutorProfile tp
        LEFT JOIN FETCH tp.user u
        LEFT JOIN FETCH tp.subjects s
        LEFT JOIN FETCH s.subject
        WHERE (:subjectId IS NULL OR EXISTS (
                SELECT 1 FROM TutorSubject ts
                WHERE ts.tutor = tp
                  AND ts.subject.id = :subjectId))
          AND (:keyword = '' OR
                LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:statuses IS NULL OR tp.status IN :statuses)
    """)
    Page<TutorProfile> searchForAdmin(
            @Param("statuses") Collection<TutorStatus> statuses,
            @Param("subjectId") Long subjectId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    long countByStatus(TutorStatus status);

    Optional<TutorProfile> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    // Dùng cho luồng tìm kiếm sau này
    @Query("""
        SELECT tp FROM TutorProfile tp
        WHERE tp.status = 'APPROVED'
          AND tp.isAvailable = true
    """)
    Page<TutorProfile> findApprovedAndAvailable(Pageable pageable);

    // Lấy chi tiết đầy đủ cho trang xem CV
    @Query("""
        SELECT tp FROM TutorProfile tp
        LEFT JOIN FETCH tp.user u
        LEFT JOIN FETCH tp.subjects s
        LEFT JOIN FETCH s.subject
        LEFT JOIN FETCH tp.certificates
        LEFT JOIN FETCH tp.availability
        LEFT JOIN FETCH tp.teachingAreas
        WHERE tp.id = :id
    """)
    Optional<TutorProfile> findByIdWithDetails(@Param("id") Long id);

    @Query("""
        SELECT tp FROM TutorProfile tp
        LEFT JOIN FETCH tp.user u
        LEFT JOIN FETCH tp.subjects s
        LEFT JOIN FETCH s.subject
        LEFT JOIN FETCH tp.certificates
        LEFT JOIN FETCH tp.availability
        LEFT JOIN FETCH tp.teachingAreas
        WHERE tp.user.id = :userId
    """)
    Optional<TutorProfile> findByUserIdWithDetails(@Param("userId") Long userId);

    // Lấy danh sách môn học hiện có trong danh sách hồ sơ
    @Query("""
        SELECT DISTINCT s FROM Subject s
        WHERE EXISTS (
            SELECT 1 FROM TutorSubject ts
            WHERE ts.subject = s
        )
        ORDER BY s.name ASC
    """)
    List<Subject> findDistinctSubjectsInProfiles();

    // Đếm số lượng gia sư đăng ký mới trong khoảng thời gian
    @Query("SELECT COUNT(tp) FROM TutorProfile tp WHERE tp.createdAt >= :fromDate AND tp.createdAt <= :toDate")
    long countByCreatedAtBetween(
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate
    );

    // Lấy danh sách gia sư theo trạng thái
    Page<TutorProfile> findByStatus(TutorStatus status, Pageable pageable);
}
