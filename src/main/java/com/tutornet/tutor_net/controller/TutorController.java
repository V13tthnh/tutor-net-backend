package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.TutorResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.TutorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long studentUserId = (userDetails != null) ? userDetails.getUser().getId() : null;
        tutorService.processTutorInvitation(tutorId, studentUserId, request);

        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/{id}")
    public TutorResponse.TutorProfileResponse getById(@PathVariable Long id) {
        return tutorService.getTutorById(id);
    }

    @PostMapping("/invitations/{invitationId}/accept")
    @PreAuthorize("hasRole('tutor')")
    public ResponseEntity<ApiResponse<Void>> acceptInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Long tutorUserId = userDetails.getUser().getId();
        tutorService.acceptTutorInvitation(invitationId, tutorUserId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
