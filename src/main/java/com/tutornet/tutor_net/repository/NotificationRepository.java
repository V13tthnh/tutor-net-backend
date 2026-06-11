package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // Tất cả — trang history (không filter)
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // Chỉ unread — trang history filter chưa đọc
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NULL ORDER BY n.createdAt DESC")
    Page<Notification> findUnreadByUserId(@Param("userId") Long userId, Pageable pageable);

    // Chỉ read — trang history filter đã đọc
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.readAt IS NOT NULL ORDER BY n.createdAt DESC")
    Page<Notification> findReadByUserId(@Param("userId") Long userId, Pageable pageable);

    // Bell dropdown — list unread không phân trang
    List<Notification> findTop20ByUserIdAndReadAtIsNullOrderByCreatedAtDesc(Long userId);

    // Badge count
    long countByUserIdAndReadAtIsNull(Long userId);

    // Mark tất cả đã đọc
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.readAt = :now
        WHERE n.user.id = :userId AND n.readAt IS NULL
        """)
    void markAllReadByUserId(@Param("userId") Long userId, @Param("now") Instant now);

    // Mark một notification đã đọc
    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.readAt = :now
        WHERE n.id = :id AND n.user.id = :userId AND n.readAt IS NULL
        """)
    void markOneReadByIdAndUserId(@Param("id") Long id,
                                  @Param("userId") Long userId,
                                  @Param("now") Instant now);
}
