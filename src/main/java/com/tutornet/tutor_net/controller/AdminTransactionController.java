package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.TransactionFilterRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.TransactionResponse;
import com.tutornet.tutor_net.dto.response.TransactionSummaryResponse;
import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/transactions")
@RequiredArgsConstructor
public class AdminTransactionController {

    private final TransactionService transactionService;

    /**
     * GET /api/v1/admin/transactions
     * Danh sách giao dịch có phân trang + lọc.
     *
     * Ví dụ Postman:
     *   ?status=SUCCESS&paymentMethod=VNPAY&search=TXN&fromDate=2025-01-01&toDate=2025-06-30&page=0&size=10
     */
    @GetMapping
    @PreAuthorize("hasAuthority('transaction:read')")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactions(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        TransactionFilterRequest filter =
                new TransactionFilterRequest(status, paymentMethod, search, fromDate, toDate, page, size);

        Page<TransactionResponse> data = transactionService.getTransactions(filter);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách giao dịch thành công", data));
    }

    /**
     * GET /api/v1/admin/transactions/summary
     * KPI tổng hợp theo cùng bộ lọc — hiển thị trên đầu bảng.
     *
     * Ví dụ: ?fromDate=2025-06-01&toDate=2025-06-30
     */
    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('transaction:summary')")
    public ResponseEntity<ApiResponse<TransactionSummaryResponse>> getSummary(
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        TransactionFilterRequest filter =
                new TransactionFilterRequest(status, paymentMethod, search, fromDate, toDate, page, size);

        TransactionSummaryResponse summary = transactionService.getSummary(filter);
        return ResponseEntity.ok(ApiResponse.ok("Lấy tổng hợp giao dịch thành công", summary));
    }

    /**
     * GET /api/v1/admin/transactions/{id}
     * Chi tiết 1 giao dịch.
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('transaction:read')")
    public ResponseEntity<ApiResponse<TransactionResponse>> getById(@PathVariable Long id) {
        TransactionResponse data = transactionService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok("Lấy chi tiết giao dịch thành công", data));
    }
}