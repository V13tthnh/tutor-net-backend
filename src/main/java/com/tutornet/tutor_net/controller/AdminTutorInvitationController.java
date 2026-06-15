package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.AdminCancelInvitationRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.enums.InvitationStatus;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.AdminTutorInvitationService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/tutor-invitations")
@RequiredArgsConstructor
public class AdminTutorInvitationController {

    private final AdminTutorInvitationService adminService;

    /**
     * API: Lấy danh sách Lời mời có lọc và phân trang dành cho Admin
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserRoleResponse.PageResponse<TutorInvitationResponse.AdminTutorInvitationTableResponse>>> getAllInvitations(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) InvitationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            // 🌟 Chuẩn hóa bộ tham số phân trang
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Dùng Utils sinh ra Pageable chuẩn của Spring
        Pageable pageable = PageableUtils.build(page, size, limit, sortBy, sortDir);

        var pageResponse = adminService.getAllInvitations(keyword, status, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.ok(pageResponse));
    }

    /**
     * API: Admin hủy ép buộc (Force Cancel) một lời mời sai phạm
     */
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('tutor_invitation:manage')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> forceCancelInvitation(
            @PathVariable Long id,
            @Valid @RequestBody AdminCancelInvitationRequest request,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        adminService.forceCancelInvitation(id, request, adminDetails.getUser().getId());

        return ResponseEntity.ok(ApiResponse.ok(
                Map.of(
                        "success", true,
                        "message", "Đã hủy lời mời thành công."
                )
        ));
    }
}