package com.tutornet.tutor_net.dto.response;
import com.tutornet.tutor_net.enums.ContractStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record AdminContractResponse(
        Long id,
        String contractNumber,
        String classCode,
        String subjectName,

        // Thông tin Gia sư
        Long tutorId,
        String tutorName,
        String tutorPhone,
        String tutorEmail,

        // Thông tin Phụ huynh / Học viên
        String contactName,
        String contactPhone,

        // Tài chính
        BigDecimal introductionFee,
        Boolean isFeePaid,
        LocalDateTime paidAt,
        LocalDate feePaymentDeadline,

        // Trạng thái & Pháp lý
        ContractStatus status,
        LocalDateTime signedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
