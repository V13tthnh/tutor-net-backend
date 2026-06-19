package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.ReviewCreateRequest;
import com.tutornet.tutor_net.dto.response.AdminReviewResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.dto.response.PublicReviewResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import org.springframework.data.domain.Page;

public interface ReviewService {
    void submitReview(ReviewCreateRequest request, CustomUserDetails currentUser);
    Page<PublicReviewResponse> getPublicReviewsByTutor(Long tutorId, int page, int size);
    Page<AdminReviewResponse> getReviewsForAdmin(Integer rating, Boolean isPublic, String search, int page, int size);
    void toggleReviewVisibility(Long reviewId);
    ContractResponse getGuestContract(Long contractId, String token);
}
