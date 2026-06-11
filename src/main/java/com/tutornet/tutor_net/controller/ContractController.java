package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ContractService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/contracts")
@RequiredArgsConstructor
public class ContractController {
    private final ContractService contractSignService;

    /**
     * POST /api/v1/contracts/{contractId}/sign
     * Gia sư bấm checkbox xác nhận ký hợp đồng điện tử (Clickwrap).
     */
    @PostMapping("/{contractId}/sign")
    @PreAuthorize("hasRole('tutor')")
    public ResponseEntity<Void> sign(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            HttpServletRequest request
    ) {
        contractSignService.signContract(contractId, request);
        return ResponseEntity.ok().build();
    }
}