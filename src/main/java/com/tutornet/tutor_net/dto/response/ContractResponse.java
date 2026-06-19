package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.ContractStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.Instant;

@Builder
public record ContractResponse(
        Long id,
        String contractNumber,

        // --- Thông tin hiển thị bảng (Mới) ---
        String classCode,
        String subjectName,
        String partnerName, // Tự động nhận diện: Tên gia sư HOẶC Tên học viên

        // --- Thông tin chi tiết (Cũ của bạn) ---
        Long targetTutorId,
        String contactName,
        String contactPhone,

        // --- Tài chính & Thời gian ---
        BigDecimal introductionFee,
        Instant effectiveDate,
        Instant feePaymentDeadline,
        Instant endDate,
        Integer freeTrialCount,

        // --- Trạng thái & File ---
        ContractStatus status,
        String contractFileUrl,
        Instant createdAt
) {}