package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
