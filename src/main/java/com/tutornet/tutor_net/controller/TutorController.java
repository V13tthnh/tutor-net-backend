package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.TutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
public class TutorController {

    private final TutorService tutorService;


    @PostMapping("/{tutorId}/invite")
    public ResponseEntity<ApiResponse<Void>> inviteTutor(
            @PathVariable Long tutorId,
            @Valid @RequestBody InviteTutorRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) { // Có thể null nếu khách vãng lai

        Long studentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;
        tutorService.processTutorInvitation(tutorId, studentUserId, request);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/invitations/{invitationId}/accept")
    // @PreAuthorize("hasRole('tutor')") // Nhớ mở comment bảo mật này nếu bạn dùng Spring Security
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // Lấy ID của user đang đăng nhập (chính là Gia sư)
        Long tutorUserId = userDetails.getUser().getId();

        tutorService.acceptTutorInvitation(invitationId, tutorUserId);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
