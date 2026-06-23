package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.TutorRequest.*;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.TutorResponse;
import com.tutornet.tutor_net.dto.response.TutorResponse.*;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.TutorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tutor/profile")
@RequiredArgsConstructor
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;

    @PostMapping
    @PreAuthorize("hasAuthority('tutor_profile:create')")
    public ResponseEntity<TutorProfileResponse> create(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody TutorProfileRequest request) {
        Long currentUserId = currentUser.getUser().getId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(tutorProfileService.createProfile(currentUserId, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('tutor_profile:read')")
    public TutorProfileResponse getMyProfile(
           @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long currentUserId = currentUser.getUser().getId();
        return tutorProfileService.getMyProfile(currentUserId);
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('tutor_profile:update')")
    public TutorProfileResponse update(
           @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody TutorProfileRequest request) {
        Long currentUserId = currentUser.getUser().getId();
        return tutorProfileService.updateProfile(currentUserId, request);
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasAuthority('tutor_profile:create')")
    public TutorSubjectResponse addSubject(
           @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody TutorSubjectRequest request) {
        Long currentUserId = currentUser.getUser().getId();
        return tutorProfileService.addSubject(currentUserId, request);
    }

    @PutMapping("/availability")
    @PreAuthorize("hasAuthority('tutor_profile:update')")
    public List<AvailabilityResponse> replaceAvailability(
           @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody List<AvailabilityRequest> requests) {
        Long currentUserId = currentUser.getUser().getId();
        return tutorProfileService.replaceAvailability(currentUserId, requests);
    }

    @PostMapping("/submit")
    @PreAuthorize("hasAuthority('tutor_profile:submit')")
    public ResponseEntity<ApiResponse<TutorProfileResponse>> submit(
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long currentUserId = currentUser.getUser().getId();
        TutorProfileResponse response = tutorProfileService.submitForReview(currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("Nộp hồ sơ thành công! Vui lòng chờ ban quản trị phê duyệt.", response));
    }

    @PostMapping("/certificates")
    @PreAuthorize("hasAuthority('tutor_profile:upload')")
    public CertificateResponse uploadCertificate(
           @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam String name,
            @RequestParam String fileUrl) { // URL sau khi upload lên S3
        Long currentUserId = currentUser.getUser().getId();
        return tutorProfileService.addCertificate(currentUserId, name, fileUrl);
    }

}