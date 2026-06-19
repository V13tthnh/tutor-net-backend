package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.request.ReviewCreateRequest;
import com.tutornet.tutor_net.dto.response.AdminReviewResponse;
import com.tutornet.tutor_net.dto.response.PublicReviewResponse;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.Review;
import com.tutornet.tutor_net.entity.User;
import org.springframework.stereotype.Component;

@Component
public class ReviewMapper {

    /**
     * Map Entity -> AdminReviewResponse
     */
    public AdminReviewResponse toAdminResponse(Review review) {
        String reviewerName = review.getReviewer() != null
                ? review.getReviewer().getFullName()
                : "Khách vãng lai (Email)";

        return AdminReviewResponse.builder()
                .id(review.getId())
                .contractId(review.getContract().getId())
                .contractNumber(review.getContract().getContractNumber())
                .tutorId(review.getTutor().getId())
                .tutorFullName(review.getTutor().getUser().getFullName())
                .tutorEmail(review.getTutor().getUser().getEmail())
                .reviewerName(reviewerName)
                .rating(review.getRating())
                .comment(review.getComment())
                .isPublic(review.getIsPublic())
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Map Entity -> PublicReviewResponse
     */
    public PublicReviewResponse toPublicResponse(Review review) {
        String reviewerName = review.getReviewer() != null
                ? review.getReviewer().getFullName()
                : "Học viên ẩn danh";

        return PublicReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .reviewerName(reviewerName)
                .createdAt(review.getCreatedAt())
                .build();
    }

    /**
     * Map DTO -> Entity (Dùng cho luồng Create)
     * Vì Entity cần các object liên kết (Contract, User) nên ta truyền thẳng vào tham số
     */
    public Review toEntity(ReviewCreateRequest request, Contract contract, User reviewer, String guestToken) {
        return Review.builder()
                .contract(contract)
                .tutor(contract.getTutor()) // Lấy trực tiếp từ Contract để đảm bảo tính nhất quán
                .reviewer(reviewer)                   // Có thể null nếu là khách
                .guestReviewToken(guestToken)         // Có thể null nếu đã đăng nhập
                .rating(request.rating())
                .comment(request.comment())
                .isPublic(true) // Mặc định luôn công khai khi vừa tạo
                .build();
    }
}