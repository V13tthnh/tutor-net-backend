package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.dto.response.ClassRequestDropdownResponse;
import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.Subject;
import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.repository.projection.CategoryCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRequestRepository extends JpaRepository<ClassRequest, Long> {
    @Query("SELECT cr FROM ClassRequest cr JOIN FETCH cr.user WHERE cr.id = :id")
    Optional<ClassRequest> findByIdWithUser(@Param("id") Long id);

    @Query(value = """
        SELECT * FROM class_requests cr
        WHERE cr.status = CAST('APPROVED' AS class_request_status)
        AND (
            :tutorId IS NULL
            OR cr.target_tutor_id IS NULL
            OR cr.target_tutor_id = :tutorId
        )
        AND (:subjectId    IS NULL OR cr.subject_id    = :subjectId)
        AND (:teachingMode IS NULL OR cr.teaching_mode = CAST(:teachingMode AS teaching_mode))
        ORDER BY cr.created_at DESC
    """,
            countQuery = """
        SELECT COUNT(*) FROM class_requests cr
        WHERE cr.status = CAST('APPROVED' AS class_request_status)
        AND (
            :tutorId IS NULL
            OR cr.target_tutor_id IS NULL
            OR cr.target_tutor_id = :tutorId
        )
        AND (:subjectId    IS NULL OR cr.subject_id    = :subjectId)
        AND (:teachingMode IS NULL OR cr.teaching_mode = CAST(:teachingMode AS teaching_mode))
    """,
            nativeQuery = true)
    Page<ClassRequest> findAvailableRequestsForJobBoard(
            @Param("tutorId")      Long tutorId,
            @Param("subjectId")    Long subjectId,
            @Param("teachingMode") String teachingMode,
            Pageable pageable
    );

    @Query(value = """
        SELECT * FROM class_requests cr
        WHERE (
            :keyword IS NULL
            OR cr.contact_name  ILIKE :keyword
            OR cr.contact_phone ILIKE :keyword
            OR cr.contact_email ILIKE :keyword
        )
        AND (:status       IS NULL OR cr.status        = CAST(:status AS class_request_status))
        AND (:subjectId    IS NULL OR cr.subject_id    = :subjectId)
        AND (:teachingMode IS NULL OR cr.teaching_mode = CAST(:teachingMode AS teaching_mode))
    """,
            countQuery = """
        SELECT COUNT(*) FROM class_requests cr
        WHERE (
            :keyword IS NULL
            OR cr.contact_name  ILIKE :keyword
            OR cr.contact_phone ILIKE :keyword
            OR cr.contact_email ILIKE :keyword
        )
        AND (:status       IS NULL OR cr.status        = CAST(:status AS class_request_status))
        AND (:subjectId    IS NULL OR cr.subject_id    = :subjectId)
        AND (:teachingMode IS NULL OR cr.teaching_mode = CAST(:teachingMode AS teaching_mode))
    """,
            nativeQuery = true)
    Page<ClassRequest> findAllForAdmin(
            @Param("keyword")      String keyword,
            @Param("status")       String status,
            @Param("subjectId")    Long subjectId,
            @Param("teachingMode") String teachingMode,
            Pageable pageable
    );

    @Query("SELECT DISTINCT cr.subject FROM ClassRequest cr")
    List<Subject> findDistinctSubjectsInRequests();

    Optional<ClassRequest> findByClassCodeAndContactPhone(String classCode, String contactPhone);

    @Query("""
            SELECT new com.tutornet.tutor_net.dto.response.ClassRequestDropdownResponse(
                c.id, c.classCode, s.name, c.gradeLevel, c.proposedPrice
            )
            FROM ClassRequest c
            JOIN c.subject s
            WHERE c.user.id = :userId 
              AND c.status IN :statuses
            ORDER BY c.createdAt DESC
            """)
    List<ClassRequestDropdownResponse> findDropdownByUserIdAndStatuses(
            @Param("userId") Long userId,
            @Param("statuses") List<ClassRequestStatus> statuses
    );

    @Query("SELECT cr FROM ClassRequest cr " +
            "LEFT JOIN cr.subject s " +
            "WHERE cr.user.id = :userId " +
            // 1. Lọc theo trạng thái (Dùng cờ boolean)
            "  AND (:hasStatus = false OR cr.status = :status) " +
            // 2. Tìm kiếm theo từ khóa
            "  AND (:hasKeyword = false OR " +
            "       LOWER(cr.classCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(cr.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(cr.gradeLevel) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<ClassRequest> searchMyClassRequests(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("hasKeyword") boolean hasKeyword,
            @Param("status") ClassRequestStatus status,
            @Param("hasStatus") boolean hasStatus,
            Pageable pageable
    );

    Page<ClassRequest> findByUserId(Long userId, Pageable pageable);

    // thống kê dashboard

    @Query("SELECT COUNT(c) FROM ClassRequest c WHERE c.createdAt BETWEEN :fromDate AND :toDate")
    long countRequestsBetweenDates(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query("SELECT COUNT(c) FROM ClassRequest c WHERE c.status = 'MATCHED' AND c.createdAt BETWEEN :fromDate AND :toDate")
    long countMatchedRequestsBetweenDates(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query(value = """
        SELECT c.status AS categoryName, COUNT(c.id) AS count
        FROM class_requests c
        WHERE c.created_at BETWEEN :fromDate AND :toDate
        GROUP BY c.status
        """, nativeQuery = true)
    List<CategoryCountProjection> getClassStatusChart(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query(value = """
        SELECT s.name AS categoryName, COUNT(c.id) AS count
        FROM class_requests c
        JOIN subjects s ON c.subject_id = s.id
        WHERE c.created_at BETWEEN :fromDate AND :toDate
        GROUP BY s.name
        ORDER BY count DESC
        LIMIT 5
        """, nativeQuery = true)
    List<CategoryCountProjection> getTopSubjectsChart(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);
}
