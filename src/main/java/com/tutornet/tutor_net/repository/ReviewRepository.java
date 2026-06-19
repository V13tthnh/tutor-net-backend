package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

     // API Phân trang và Bộ lọc nâng cao dành cho Admin
     @Query("SELECT r FROM Review r " +
             "LEFT JOIN r.contract c " +
             "LEFT JOIN r.tutor t " +
             "LEFT JOIN r.reviewer u " +
             "WHERE (:rating IS NULL OR r.rating = :rating) " +
             "  AND (:isPublic IS NULL OR r.isPublic = :isPublic) " +
             "  AND (:hasSearch = false OR LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :search, '%')) " +
             "       OR LOWER(t.user.fullName) LIKE LOWER(CONCAT('%', :search, '%')) " +
             "       OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%'))" +
             "  )")
     Page<Review> findAllForAdmin(
             @Param("rating") Integer rating,
             @Param("isPublic") Boolean isPublic,
             @Param("search") String search,
             @Param("hasSearch") boolean hasSearch,
             Pageable pageable
     );

    // Kiểm tra xem hợp đồng đã được đánh giá chưa
    boolean existsByContractId(Long contractId);

    // Kiểm tra token có thuộc đúng contract không
    Optional<Review> findByGuestReviewToken(String token);

    // Kiểm tra xem token này đã được sử dụng chưa (chống submit 2 lần)
    boolean existsByGuestReviewToken(String token);

    // Truy vấn tính toán số sao trung bình của gia sư
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.tutor.user.id = :tutorId AND r.isPublic = true")
    Double calculateAverageRatingByTutorId(@Param("tutorId") Long tutorId);

    // Đếm tổng số đánh giá công khai
    @Query("SELECT COUNT(r) FROM Review r WHERE r.tutor.user.id = :tutorId AND r.isPublic = true")
    Integer countPublicReviewsByTutorId(@Param("tutorId") Long tutorId);

    Page<Review> findByTutorIdAndIsPublicTrue(Long tutorId, Pageable pageable);

    @Query("SELECT r FROM Review r JOIN FETCH r.tutor t WHERE r.rating <= 2 ORDER BY r.createdAt DESC")
    List<Review> findTop5NegativeReviews(Pageable pageable);
}