package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.ClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.CreateClassRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ClassRequestResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ClassRequestService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/class-requests")
@RequiredArgsConstructor
public class ClassRequestController {

    private final ClassRequestService classRequestService;

    /**
     * GET /api/v1/public/class-requests
     * ?subjectId=1
     * &teachingMode=ONLINE
     * &page=1&size=12
     * &sortBy=createdAt&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserRoleResponse.PageResponse<ClassRequestResponse>>> getJobBoard(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String teachingMode,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long authenticatedUserId = (currentUser != null) ? currentUser.getUser().getId() : null;
        Pageable pageable = PageableUtils.build(page, size, null, sortBy, sortDir);
        UserRoleResponse.PageResponse<ClassRequestResponse> responsePage = classRequestService.getJobBoardRequests(
                authenticatedUserId,
                subjectId,
                teachingMode,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(responsePage));
    }

    /**
     * API Công khai tiếp nhận form đăng lớp từ người dùng (Học viên/Phụ huynh/Khách vãng lai)
     * POST /api/v1/public/class-requests
     */
    @PostMapping
    public ResponseEntity<ClassRequestResponse> postClassRequest(
            @Valid @RequestBody CreateClassRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long authenticatedUserId = (currentUser != null) ? currentUser.getUser().getId() : null;
        ClassRequestResponse response = classRequestService.createClassRequest(request, authenticatedUserId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/invite-tutor") // Hoặc endpoint tương tự
    public ResponseEntity<ApiResponse<ClassRequestResponse>> inviteTutor(
            @Valid @RequestBody CreateClassRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser) {
        Long authenticatedUserId = (currentUser != null) ? currentUser.getUser().getId() : null;
        ClassRequestResponse response = classRequestService.createClassRequest(request, authenticatedUserId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<List<ClassRequestResponse>>> postBulkClassRequests(
            @Valid @RequestBody ClassRequest.BulkClassRequest bulkRequest,
            @AuthenticationPrincipal CustomUserDetails currentUser) {

        Long userId = (currentUser != null) ? currentUser.getUser().getId() : null;
        List<ClassRequestResponse> responses = classRequestService.createBulkClassRequests(bulkRequest.requests(), userId);
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }
}
