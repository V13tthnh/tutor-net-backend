package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.TutorInvitation;
import com.tutornet.tutor_net.enums.InvitationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TutorInvitationRepository extends JpaRepository<TutorInvitation, Long> {
    /**
     * Lấy danh sách lời mời theo tutor_id, hỗ trợ phân trang.
     * Dùng index idx_tutor_invitations_tutor_id → truy vấn nhanh.
     */
    Page<TutorInvitation> findByTutor_IdOrderByCreatedAtDesc(Long tutorId, Pageable pageable);

    /**
     * Lọc thêm theo status (PENDING / ACCEPTED / REJECTED).
     */
    @Query("""
            SELECT ti FROM TutorInvitation ti
            WHERE ti.tutor.id = :tutorId
              AND (:status IS NULL OR ti.status = :status)
            ORDER BY ti.createdAt DESC
            """)
    Page<TutorInvitation> findByTutorIdAndStatus(
            @Param("tutorId") Long tutorId,
            @Param("status") InvitationStatus status,
            Pageable pageable
    );
}