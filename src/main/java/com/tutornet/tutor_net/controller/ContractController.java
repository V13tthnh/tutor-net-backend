package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ContractService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractService;

    /**
     * GET /api/v1/contracts
     * Lấy danh sách hợp đồng cá nhân có lọc, tìm kiếm và phân trang
     */
    @GetMapping
    @PreAuthorize("hasAuthority('contract:read')")
    public ResponseEntity<ApiResponse<Page<ContractResponse>>> getMyContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableUtils.build(page, size, limit, sortBy, sortDir);
        // Gọi Service trả về Page
        Page<ContractResponse> responses = contractService.getMyContracts(
                userDetails.getUser().getId(),
                keyword,
                status,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.ok(responses));
    }

    /**
     * POST /api/v1/contracts/{contractId}/sign
     * Gia sư bấm checkbox xác nhận ký hợp đồng điện tử
     */
    @PostMapping("/{contractId}/sign")
    @PreAuthorize("hasAuthority('contract:sign')")
    public ResponseEntity<Void> sign(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isBlank()) ipAddress = request.getRemoteAddr();
        contractService.signContractAndGeneratePdf(contractId, ipAddress, userDetails.getUser().getId());
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/contracts/{contractId}/download-pdf
     * Tải file hợp đồng PDF (dành cho người dùng thường)
     */
    @GetMapping("/{contractId}/download-pdf")
    @PreAuthorize("hasAuthority('contract:read')")
    public void downloadContractPdf(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        contractService.exportContractPdfForUser(contractId, userDetails.getUser().getId(), response);
    }

    /**
     * POST /api/v1/contracts/{contractId}/complete
     * Học viên bấm hoàn thành khóa học để nhận link đánh giá trực tiếp (Phục vụ cả nghiệp vụ và Demo)
     */
    @PostMapping("/{contractId}/complete")
    @PreAuthorize("hasAuthority('contract:read')")
    public ResponseEntity<ApiResponse<String>> completeContract(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        String token = contractService.completeContractByStudent(contractId, userDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok("Kết thúc khóa học thành công. Vui lòng đánh giá gia sư!", token));
    }
}