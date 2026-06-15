    package com.tutornet.tutor_net.controller;

    import com.tutornet.tutor_net.dto.request.RejectInvitationRequest;
    import com.tutornet.tutor_net.dto.response.ApiResponse;
    import com.tutornet.tutor_net.dto.response.ContractPreviewResponse;
    import com.tutornet.tutor_net.dto.response.TutorInvitationResponse;
    import com.tutornet.tutor_net.enums.InvitationStatus;
    import com.tutornet.tutor_net.security.CustomUserDetails;
    import com.tutornet.tutor_net.service.ContractService;
    import com.tutornet.tutor_net.service.TutorInvitationService;
    import com.tutornet.tutor_net.util.PageableUtils;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.validation.Valid;
    import lombok.RequiredArgsConstructor;
    import org.springframework.data.domain.Page;
    import org.springframework.data.domain.Pageable;
    import org.springframework.http.ResponseEntity;
    import org.springframework.security.access.prepost.PreAuthorize;
    import org.springframework.security.core.annotation.AuthenticationPrincipal;
    import org.springframework.web.bind.annotation.*;

    @RestController
    @RequestMapping("/api/v1/tutor/invitations")
    @RequiredArgsConstructor
    public class TutorInvitationController {

        private final TutorInvitationService invitationService;
        private final ContractService contractService;
        /**
         * GET /api/v1/tutor/invitations
         *
         * Gia sư xem danh sách lời mời dạy của mình.
         *
         * Query params:
         *   status  – PENDING | ACCEPTED | REJECTED (tuỳ chọn, bỏ trống = lấy tất cả)
         *   page    – số trang, bắt đầu từ 1 (mặc định 1)
         *   size    – số bản ghi / trang (mặc định 10, tối đa 100)
         *   sortBy  – tên field sắp xếp (mặc định createdAt)
         *   sortDir – asc | desc (mặc định desc)
         */
        @GetMapping
        @PreAuthorize("hasAuthority('tutor:read')")
        public ResponseEntity<Page<TutorInvitationResponse>> getMyInvitations(
                @AuthenticationPrincipal CustomUserDetails userDetails,
                @RequestParam(required = false) InvitationStatus status,
                @RequestParam(required = false) Integer page,
                @RequestParam(required = false) Integer size,
                @RequestParam(required = false) String sortBy,
                @RequestParam(required = false) String sortDir
        ) {
            Pageable pageable = PageableUtils.build(page, size, null, sortBy, sortDir);

            Page<TutorInvitationResponse> result =
                    invitationService.getMyInvitations(userDetails.getUser().getId(), status, pageable);

            return ResponseEntity.ok(result);
        }

        @GetMapping("/{id}/contract-preview")
        @PreAuthorize("hasAuthority('tutor:read')")
        public ResponseEntity<ApiResponse<ContractPreviewResponse>> getContractPreview(
                @PathVariable Long id,
                @AuthenticationPrincipal CustomUserDetails userDetails
        ) {
            ContractPreviewResponse preview = invitationService.getContractPreview(id, userDetails.getUser().getId());
            return ResponseEntity.ok(ApiResponse.ok(preview));
        }

        @PostMapping("/{id}/accept-and-sign")
        @PreAuthorize("hasAuthority('tutor:read')")
        public ResponseEntity<ApiResponse<Void>> acceptAndSign(
                @PathVariable Long id,
                HttpServletRequest request,
                @AuthenticationPrincipal CustomUserDetails userDetails
        ) {
            // Trích xuất IP
            String ipAddress = request.getHeader("X-Forwarded-For");
            if (ipAddress == null || ipAddress.isBlank()) ipAddress = request.getRemoteAddr();

            // Chạy All-in-one
            invitationService.acceptAndSignContract(id, userDetails.getUser().getId(), ipAddress);

            return ResponseEntity.ok(ApiResponse.ok(null));
        }


        @PostMapping("/{id}/reject")
        @PreAuthorize("hasAuthority('tutor:read')")
        public ResponseEntity<ApiResponse<Void>> reject(
                @PathVariable Long id,
                @AuthenticationPrincipal CustomUserDetails userDetails,
                @Valid @RequestBody(required = false) RejectInvitationRequest body
        ) {
            String reason = (body != null) ? body.getRejectionReason() : null;
            invitationService.rejectInvitation(id,  userDetails.getUser().getId(), reason);
            return ResponseEntity.ok(ApiResponse.ok(null));
        }
    }