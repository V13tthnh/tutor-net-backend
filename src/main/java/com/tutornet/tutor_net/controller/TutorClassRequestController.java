package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ClassApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/tutors/class-requests")
@RequiredArgsConstructor
public class TutorClassRequestController {

    private final ClassApplicationService applicationService;

    /**
     * API để Gia sư thao tác "Đồng ý" hoặc "Từ chối" lời mời dạy
     * POST /api/v1/tutors/class-requests/{id}/respond
     */
    @PostMapping("/{id}/respond")
    @PreAuthorize("hasRole('tutor')") // Chỉ tài khoản có Role Gia sư mới được gọi
    public ResponseEntity<ApiResponse<ClassApplicationResponse>> respondToInvite(
            @PathVariable("id") Long requestId,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        // Lấy quyết định của gia sư từ payload (true = đồng ý, false = từ chối)
        boolean isAccepted = (Boolean) payload.getOrDefault("isAccepted", false);
        String message = (String) payload.getOrDefault("message", "");

        ClassApplicationResponse response = applicationService.respondToDirectInvite(
                requestId,
                isAccepted,
                message,
                currentUser.getUser().getId()
        );

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
