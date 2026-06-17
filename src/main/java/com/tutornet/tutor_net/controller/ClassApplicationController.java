package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.ApplicationRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ClassApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/class-requests/{classRequestId}/applications")
@RequiredArgsConstructor
public class ClassApplicationController {

    private final ClassApplicationService applicationService;

    /**
     * Gia sư ứng tuyển vào một lớp học
     */
    @PostMapping
    @PreAuthorize("hasAuthority('tutor:read')")
    public ResponseEntity<ApiResponse<ClassApplicationResponse>> applyForClass(
            @PathVariable Long classRequestId,
            @Valid @RequestBody(required = false) ApplicationRequest.ApplyClassRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ClassApplicationResponse response = applicationService.applyForClass(classRequestId, request, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok("Ứng tuyển thành công", response));    }

    /**
     * Học viên xem danh sách các gia sư đã ứng tuyển vào lớp của mình
     */
    @GetMapping
    @PreAuthorize("hasAuthority('student_request:read')")
    public ResponseEntity<ApiResponse<List<ClassApplicationResponse>>> getApplicationsForClass(
            @PathVariable Long classRequestId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ClassApplicationResponse> responses = applicationService.getApplicationsForClass(classRequestId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * Học viên CHỐT GIA SƯ (Phê duyệt 1 đơn ứng tuyển)
     */
    @PostMapping("/{applicationId}/accept")
    @PreAuthorize("hasAuthority('student_request:apply')")
    public ResponseEntity<ApiResponse<ClassApplicationResponse>> acceptApplication(
            @PathVariable Long classRequestId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        ClassApplicationResponse response = applicationService.acceptApplication(classRequestId, applicationId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã chốt gia sư thành công! Hợp đồng đang chờ gia sư ký nhận.", response));
    }
}