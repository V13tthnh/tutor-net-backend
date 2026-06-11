package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.EnumOptionRequest;
import com.tutornet.tutor_net.dto.request.TutorRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.TutorResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.enums.TutorStatus;
import com.tutornet.tutor_net.enums.UserStatus;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.AdminTutorService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tutors")
@RequiredArgsConstructor
public class AdminTutorController {
    private final AdminTutorService adminTutorService;

    // GET /api/v1/admin/tutors?keyword=&status=&subjectId=&page=0&size=20
    @GetMapping
    @PreAuthorize("hasAuthority('tutor:read')")
    public ResponseEntity<ApiResponse<UserRoleResponse.PageResponse<TutorResponse.TutorSummaryResponse>>> list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) List<TutorStatus> statuses,
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(defaultValue = "1")  int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
            ) {
        Pageable pageable = PageableUtils.build(page, size, limit, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok(
                adminTutorService.list(keyword, statuses, subjectIds, pageable)));
    }

    // GET /api/v1/admin/tutors/stats
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('tutor:read')")
    public TutorResponse.TutorStatsResponse stats() {
        return adminTutorService.getStats();
    }

    // GET /api/v1/admin/tutors/{id}  — xem CV đầy đủ
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('tutor:read')")
    public TutorResponse.TutorProfileResponse getById(@PathVariable Long id) {
        return adminTutorService.getTutorById(id);
    }

    // POST /api/v1/admin/tutors/{id}/review  — duyệt hoặc từ chối
    @PostMapping("/{id}/review")
    @PreAuthorize("hasAuthority('tutor:review')")
    public ResponseEntity<ApiResponse<TutorResponse.TutorProfileResponse>> review(
            @PathVariable Long id,
            @Valid @RequestBody TutorRequest.ReviewTutorRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công",
                adminTutorService.reviewTutor(id, request, currentUser.getUser().getId())));
    }

    // PATCH /api/v1/admin/tutors/{id}/suspend
    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('tutor:manage')")
    public ResponseEntity<Void> suspend(
            @PathVariable Long id,
            @RequestParam String reason) {
        adminTutorService.suspendTutor(id, reason);
        return ResponseEntity.noContent().build();
    }

    // PATCH /api/v1/admin/tutors/{id}/unsuspend
    @PatchMapping("/{id}/unsuspend")
    @PreAuthorize("hasAuthority('tutor:manage')")
    public ResponseEntity<Void> unsuspend(@PathVariable Long id) {
        adminTutorService.unsuspendTutor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/statuses")
    @PreAuthorize("hasAuthority('tutor:read')")
    public List<EnumOptionRequest> getStatuses() {
        return Arrays.stream(TutorStatus.values())
                .map(status -> new EnumOptionRequest(
                        status.name(),
                        status.getLabel()
                ))
                .toList();
    }

    @GetMapping("/filter-options")
    @PreAuthorize("hasAuthority('tutor:read')")
    public TutorResponse.TutorFilterOptionsResponse filterOptions() {
        return adminTutorService.getFilterOptions();
    }
}
