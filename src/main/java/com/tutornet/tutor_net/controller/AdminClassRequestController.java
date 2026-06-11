package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.ClassRequest.BulkReviewClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.ReviewClassRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ClassRequestFilterOptionsResponse;
import com.tutornet.tutor_net.dto.response.ClassRequestResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ClassRequestService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/class-requests")
@RequiredArgsConstructor
public class AdminClassRequestController {

    private final ClassRequestService classRequestService;

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/admin/class-requests
    //   ?status=PENDING          ← lọc theo trạng thái (tuỳ chọn)
    //   &subjectId=1
    //   &teachingMode=ONLINE
    //   &page=1&size=12
    //   &sortBy=createdAt&sortDir=desc
    // ─────────────────────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAuthority('class_request:read')")
    public ResponseEntity<ApiResponse<UserRoleResponse.PageResponse<ClassRequestResponse>>> getAllForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String teachingMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Pageable pageable = PageableUtils.build(page, size, null, sortBy, sortDir);

        UserRoleResponse.PageResponse<ClassRequestResponse> responsePage =
                classRequestService.getAllRequestsForAdmin(keyword, status, subjectId, teachingMode, pageable);

        return ResponseEntity.ok(ApiResponse.ok(responsePage));
    }

    // ─────────────────────────────────────────────────────────
    // GET /api/v1/admin/class-requests/{id}
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('class_request:read')")
    public ResponseEntity<ApiResponse<ClassRequestResponse>> getDetail(@PathVariable Long id) {
        ClassRequestResponse response = classRequestService.getRequestDetailForAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/admin/class-requests/{id}/review
    // ─────────────────────────────────────────────────────────
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAuthority('class_request:review')")
    public ResponseEntity<ApiResponse<ClassRequestResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody ReviewClassRequest reviewRequest,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Long adminId = currentUser.getUser().getId();
        ClassRequestResponse response = classRequestService.reviewClassRequest(id, reviewRequest, adminId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/admin/class-requests/filter-options
    // ─────────────────────────────────────────────────────────
    @GetMapping("/filter-options")
    @PreAuthorize("hasAuthority('class_request:read')")
    public ResponseEntity<ApiResponse<ClassRequestFilterOptionsResponse>> getFilterOptions() {
        ClassRequestFilterOptionsResponse response = classRequestService.getClassRequestFilterOptions();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ─────────────────────────────────────────────────────────
    // PATCH /api/v1/admin/class-requests/bulk-review
    // ─────────────────────────────────────────────────────────
    @PatchMapping("/bulk-review")
    @PreAuthorize("hasAuthority('class_request:review')")
    public ResponseEntity<ApiResponse<List<ClassRequestResponse>>> bulkReview(
            @Valid @RequestBody BulkReviewClassRequest bulkRequest,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Long adminId = currentUser.getUser().getId();
        List<ClassRequestResponse> responses = classRequestService.reviewBulkClassRequests(bulkRequest, adminId);

        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}