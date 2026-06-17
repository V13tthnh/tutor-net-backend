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
            // 🌟 Chuẩn hóa bộ tham số phân trang
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        // Build Pageable
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
     * Gia sư bấm checkbox xác nhận ký hợp đồng điện tử (Clickwrap).
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
        contractService.signContractAndGeneratePdf(contractId, ipAddress);
        return ResponseEntity.ok().build();
    }
}