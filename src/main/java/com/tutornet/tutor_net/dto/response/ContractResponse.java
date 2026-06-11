package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.ContractStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final record ContractResponse(
        Long id,
        String contractNumber,
        Long requestId,
        String studentName,
        String studentPhone,
        Long tutorId,
        String tutorName,
        BigDecimal introductionFee,
        LocalDate effectiveDate,
        LocalDate feePaymentDeadline,
        ContractStatus status,
        String contractFileUrl,
        Integer freeTrialCount,
        LocalDateTime createdAt
) {}