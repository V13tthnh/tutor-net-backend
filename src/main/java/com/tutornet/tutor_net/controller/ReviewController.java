package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.ReviewCreateRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.dto.response.PublicReviewResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * API: Lấy danh sách đánh giá công khai của 1 Gia sư
     * Endpoint: GET /api/v1/public/reviews/tutor/10?page=0&size=5
     * LƯU Ý: Đảm bảo đường dẫn này được permitAll() trong SecurityConfig
     */
    @GetMapping("/tutor/{tutorId}")
    public ResponseEntity<ApiResponse<Page<PublicReviewResponse>>> getPublicReviewsByTutor(
            @PathVariable Long tutorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        Page<PublicReviewResponse> data = reviewService.getPublicReviewsByTutor(tutorId, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách đánh giá thành công", data));
    }

    /**
     * API: Lấy thông tin hợp đồng dành cho khách đánh giá từ Email (Magic Link)
     * Endpoint: GET /api/v1/reviews/guest-contract?contractId=11&token=eb47c5ef-2d6f-4ecd-ab0e-460f7968a329-11
     */
    @GetMapping("/guest-contract")
    public ResponseEntity<ApiResponse<ContractResponse>> getGuestContract(
            @RequestParam Long contractId,
            @RequestParam String token
    ) {
        ContractResponse data = reviewService.getGuestContract(contractId, token);
        return ResponseEntity.ok(ApiResponse.ok("Tải thông tin hợp đồng thành công", data));
    }

    /**
     * API Gửi Đánh giá (Hỗ trợ cả người dùng đăng nhập VÀ khách vãng lai)
     * LƯU Ý: Thêm .requestMatchers(HttpMethod.POST, "/api/v1/reviews").permitAll() vào SecurityConfig
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitReview(
            @Valid @RequestBody ReviewCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails // Sẽ là null nếu client không gửi Token JWT
    ) {
        reviewService.submitReview(request, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Cảm ơn bạn đã gửi đánh giá gia sư!", null));
    }
}