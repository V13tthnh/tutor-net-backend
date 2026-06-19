package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response trả về cho Admin — đầy đủ thông tin 1 giao dịch.
 */
public record TransactionResponse(
        Long id,
        String transactionCode,      // "TXN1718123456789"
        String gatewayReference,     // Mã tham chiếu từ VNPay / PayOS

        // Thông tin hợp đồng liên quan
        Long contractId,
        String contractNumber,       // "HD-2025-001"

        // Thông tin gia sư (người thực hiện thanh toán)
        Long userId,
        String tutorName,
        String tutorEmail,

        BigDecimal amount,
        PaymentMethod paymentMethod,
        TransactionStatus status,
        String note,

        Instant paidAt,
        Instant createdAt,
        Instant updatedAt
) {}