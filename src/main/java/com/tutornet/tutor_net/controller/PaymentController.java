package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.config.VNPayConfig;
import com.tutornet.tutor_net.dto.request.PaymentCreateRequest;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.PaymentResponse;
import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.entity.Transaction;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.repository.ContractRepository;
import com.tutornet.tutor_net.repository.TransactionRepository;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final TransactionRepository transactionRepository;
    private final ContractRepository contractRepository;
    private final VNPayConfig vnPayConfig;

    @PostMapping("/create-url")
    @PreAuthorize("hasAuthority('contract:read')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createPaymentUrl(
            @Valid @RequestBody PaymentCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        PaymentResponse response = paymentService.createPaymentUrl(request, userDetails);
        return ResponseEntity.ok(ApiResponse.ok("Khởi tạo URL VNPay thành công", response));
    }

    /**
     * Webhook IPN VNPay (Phải cấu hình PUBLIC trong SecurityConfig)
     * VNPay gọi IPN qua method GET thay vì POST
     */
    @GetMapping("/webhook/vnpay_ipn")
    @Transactional
    public ResponseEntity<Map<String, String>> vnpayIpn(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();
        try {
            // ✅ Bug 1 fixed — đọc tên param gốc, encode VALUE (không encode tên)
            Map<String, String> fields = new HashMap<>();
            for (Enumeration<String> params = request.getParameterNames(); params.hasMoreElements();) {
                String fieldName  = params.nextElement();                    // tên gốc
                String fieldValue = request.getParameter(fieldName);        // value gốc
                if (fieldValue != null && !fieldValue.isEmpty()) {
                    fields.put(fieldName, fieldValue);
                }
            }

            // Lấy chữ ký gốc TRƯỚC khi xóa
            String vnpSecureHash = fields.remove("vnp_SecureHash");
            fields.remove("vnp_SecureHashType");

            // ✅ Bug 2 fixed — encode value khi build hash string
            String signValue = vnPayConfig.hmacSHA512(vnPayConfig.secretKey, hashAllFields(fields));
            if (!signValue.equals(vnpSecureHash)) {
                log.warn("VNPay IPN - Invalid checksum. Expected: {}, Got: {}", signValue, vnpSecureHash);
                response.put("RspCode", "97");
                response.put("Message", "Invalid Checksum");
                return ResponseEntity.ok(response);
            }

            String transactionCode  = fields.get("vnp_TxnRef");
            String vnpResponseCode  = fields.get("vnp_ResponseCode");
            String vnpTransactionNo = fields.get("vnp_TransactionNo");

            Transaction txn = transactionRepository.findByTransactionCode(transactionCode).orElse(null);
            if (txn == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return ResponseEntity.ok(response);
            }

            // Kiểm tra số tiền
            long expectedAmount = txn.getAmount().longValue() * 100L;
            long receivedAmount = Long.parseLong(fields.get("vnp_Amount"));
            if (expectedAmount != receivedAmount) {
                response.put("RspCode", "04");
                response.put("Message", "Invalid amount");
                return ResponseEntity.ok(response);
            }

            // Chống xử lý trùng lặp
            if (txn.getStatus() == TransactionStatus.SUCCESS) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return ResponseEntity.ok(response);
            }

            if ("00".equals(vnpResponseCode)) {
                txn.setStatus(TransactionStatus.SUCCESS);
                txn.setGatewayReference(vnpTransactionNo);
                txn.setPaidAt(Instant.now());
                transactionRepository.save(txn);

                Contract contract = txn.getContract();
                contract.setIsFeePaid(true);
                contract.setPaidAt(Instant.now());
                contractRepository.save(contract);

                log.info("VNPay IPN - Thanh toán thành công. TxnRef: {}, HD: {}",
                        transactionCode, contract.getContractNumber());
            } else {
                txn.setStatus(TransactionStatus.FAILED);
                txn.setNote("VNPay ResponseCode: " + vnpResponseCode);
                transactionRepository.save(txn);
                log.warn("VNPay IPN - Thanh toán thất bại. TxnRef: {}, Code: {}",
                        transactionCode, vnpResponseCode);
            }

            response.put("RspCode", "00");
            response.put("Message", "Confirm Success");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Lỗi IPN VNPay: ", e);
            response.put("RspCode", "99");
            response.put("Message", "Unknown error");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * API Công khai trung gian phản hồi lượt click từ email chăm sóc khách hàng
     * GET /api/v1/payments/click-pay-email?contractNumber=HD-2026-XYZ
     */
    @GetMapping("/click-pay-email")
    public void redirectPaymentFromEmail(
            @RequestParam String contractNumber,
            jakarta.servlet.http.HttpServletResponse response
    ) throws java.io.IOException {
        // Sinh ra link thanh toán mới tinh không lo hết hạn
        String vnpayUrl = paymentService.createPaymentUrlFromEmail(contractNumber);

        // Ra lệnh cho trình duyệt chuyển hướng thẳng sang cổng thanh toán VNPay luôn
        response.sendRedirect(vnpayUrl);
    }

    // Hàm phụ trợ nối chuỗi để check lại checksum của VNPay
    private String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName  = itr.next();
            String fieldValue = fields.get(fieldName);
            if (fieldValue != null && !fieldValue.isEmpty()) {
                sb.append(fieldName)
                        .append("=")
                        .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
            }
            if (itr.hasNext()) sb.append("&");
        }
        return sb.toString();
    }
}