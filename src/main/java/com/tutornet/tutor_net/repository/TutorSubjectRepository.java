package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.TutorSubject;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TutorSubjectRepository extends JpaRepository<TutorSubject, Long> {
    // Lấy tất cả môn học của một gia sư
    List<TutorSubject> findByTutorId(Long tutorId);

    // Kiểm tra xem gia sư đã có môn học chưa
    boolean existsByTutorIdAndSubjectId(Long tutorId, Long subjectId);

    // Xóa môn học của gia sư
    void deleteByTutorIdAndSubjectId(Long tutorId, Long subjectId);

    /**
     * Tìm môn học của gia sư theo tutorId và subjectId
     * Trả về Optional để xử lý trường hợp không tìm thấy
     */
    Optional<TutorSubject> findByTutorIdAndSubjectId(Long tutorId, Long subjectId);

    /**
     * Lấy tất cả môn học của gia sư kèm theo thông tin môn học (Eager loading)
     * Dùng để hiển thị danh sách môn học kèm tên môn
     */
    @Query("SELECT ts FROM TutorSubject ts " +
            "JOIN FETCH ts.subject s " +
            "WHERE ts.tutor.id = :tutorId")
    List<TutorSubject> findByTutorIdWithSubject(Long tutorId);

    /**
     * Đếm số lượng môn học của một gia sư
     */
    long countByTutorId(Long tutorId);

    /**
     * Xóa tất cả môn học của một gia sư (dùng khi xóa/xử lý hồ sơ)
     */
    @Modifying
    @Transactional
    void deleteByTutorId(Long tutorId);

    /**
     * Tìm môn học của gia sư theo subject slug
     */
    @Query("SELECT ts FROM TutorSubject ts " +
            "JOIN ts.subject s " +
            "WHERE ts.tutor.id = :tutorId AND s.slug = :slug")
    Optional<TutorSubject> findByTutorIdAndSubjectSlug(@Param("tutorId") Long tutorId,
                                                       @Param("slug") String slug);
    /**
     * Kiểm tra tồn tại theo tutorId và subject slug
     */
    @Query("SELECT COUNT(ts) > 0 FROM TutorSubject ts " +
            "JOIN ts.subject s " +
            "WHERE ts.tutor.id = :tutorId AND s.slug = :slug")
    boolean existsByTutorIdAndSubjectSlug(@Param("tutorId") Long tutorId,
                                          @Param("slug") String slug);

}
