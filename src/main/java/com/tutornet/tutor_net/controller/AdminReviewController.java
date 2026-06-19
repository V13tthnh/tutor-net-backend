package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.AdminReviewResponse;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {

    private final ReviewService reviewService;

    /**
     * Lấy danh sách đánh giá phân trang kèm bộ lọc đa năng cho Data Table Admin
     * GET /api/v1/admin/reviews?rating=5&isPublic=true&search=Thành&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAuthority('review:read')")
    public ResponseEntity<ApiResponse<Page<AdminReviewResponse>>> getAllReviewsForAdmin(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean isPublic,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<AdminReviewResponse> data = reviewService.getReviewsForAdmin(rating, isPublic, search, page, size);
        return ResponseEntity.ok(ApiResponse.ok("Tải danh sách kiểm duyệt đánh giá thành công", data));
    }

    /**
     * Bật/Tắt trạng thái công khai của một đánh giá (Ẩn review vi phạm)
     * PATCH /api/v1/admin/reviews/15/toggle-visibility
     */
    @PatchMapping("/{id}/toggle-visibility")
    @PreAuthorize("hasAuthority('review:update')")
    public ResponseEntity<ApiResponse<Void>> toggleReviewVisibility(@PathVariable Long id) {
        reviewService.toggleReviewVisibility(id);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật trạng thái hiển thị đánh giá thành công", null));
    }
}
