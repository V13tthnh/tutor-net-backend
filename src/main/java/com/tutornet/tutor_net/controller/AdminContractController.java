package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.ContractDisputeRequest;
import com.tutornet.tutor_net.dto.response.AdminContractResponse;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.ContractResponse;
import com.tutornet.tutor_net.enums.ContractStatus;
import com.tutornet.tutor_net.export.excel.ContractExcelExporter;
import com.tutornet.tutor_net.export.pdf.ContractPdfGenerator;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.ContractService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/contracts")
@RequiredArgsConstructor
public class AdminContractController {
    private final ContractService contractService;

    private final ContractExcelExporter contractExcelExporter;
    private final ContractPdfGenerator contractPdfGenerator;

    /**
     * Lấy danh sách hợp đồng cho Admin (Tìm kiếm + Lọc dòng tiền + Phân trang)
     * Thúc đẩy tiến độ: Lọc status = PENDING_SIGNATURE, sortBy = createdAt, sortDir = asc để tìm hợp đồng bị ngâm lâu nhất.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('tutor:manage') or hasAuthority('contract:read')")
    public ResponseEntity<ApiResponse<Page<AdminContractResponse>>> getAllContractsForAdmin(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) Boolean isFeePaid,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableUtils.build(page, size, null, sortBy, sortDir);
        Page<AdminContractResponse> response = contractService.getContractsForAdmin(keyword, status, isFeePaid, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Thao tác cốt lõi: Xác nhận đã thu phí chuyển khoản thủ công từ gia sư
     */
    @PostMapping("/{contractId}/confirm-payment")
    @PreAuthorize("hasAuthority('tutor:manage')")
    public ResponseEntity<ApiResponse<Void>> confirmPayment(
            @PathVariable Long contractId,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        contractService.confirmPaymentByAdmin(contractId, adminDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok("Xác nhận thu tiền phí nhận lớp thành công", null));
    }

    /**
     * Xử lý sự cố & Bảo hành học thử hỏng (Hủy / Đánh dấu vi phạm kèm hoàn phí nếu có)
     */
    @PostMapping("/{contractId}/dispute")
    @PreAuthorize("hasAuthority('tutor:manage')")
    public ResponseEntity<ApiResponse<Void>> resolveDispute(
            @PathVariable Long contractId,
            @Valid @RequestBody ContractDisputeRequest disputeRequest,
            @AuthenticationPrincipal CustomUserDetails adminDetails
    ) {
        contractService.resolveContractDispute(contractId, disputeRequest, adminDetails.getUser().getId());
        return ResponseEntity.ok(ApiResponse.ok("Xử lý tranh chấp hợp đồng thành công.", null));
    }

    /**
     * TẢI FILE EXCEL: Danh sách dữ liệu thô phục vụ đối soát kế toán
     */
    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority('contract:export')")
    public void exportContractsToExcel(
            @RequestParam(required = false) ContractStatus status,
            @RequestParam(required = false) Boolean isFeePaid,
            HttpServletResponse response
    ) throws IOException {
        // Cấu hình HTTP Header
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=Contracts_" + System.currentTimeMillis() + ".xlsx");

        // Lấy danh sách từ DB
        List<AdminContractResponse> list = contractService.getContractsForExport(status, isFeePaid);

        // Gọi Excel Template Method
        contractExcelExporter.export(list, response);
    }

    @GetMapping("/{contractId}/download-pdf")
    @PreAuthorize("hasAuthority('contract:read')")
    public void downloadContractPdf(
            @PathVariable Long contractId,
            HttpServletResponse response
    ) {
        contractService.exportContractPdf(contractId, response);
    }

    /**
     * Kết thúc hợp đồng thủ công và kích hoạt luồng gửi email xin đánh giá
     */
    @PostMapping("/{contractId}/complete")
    @PreAuthorize("hasAuthority('tutor:manage')")
    public ResponseEntity<ApiResponse<Void>> completeContract(
            @PathVariable Long contractId
    ) {
        contractService.completeContract(contractId);
        return ResponseEntity.ok(ApiResponse.ok("Hoàn thành hợp đồng và kích hoạt yêu cầu đánh giá thành công.", null));
    }
}
