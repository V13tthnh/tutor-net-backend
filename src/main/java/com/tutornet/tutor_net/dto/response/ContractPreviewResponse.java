package com.tutornet.tutor_net.dto.response;

import java.math.BigDecimal;

public record ContractPreviewResponse(
        String tutorName,
        Integer tutorBirthYear,
        String tutorPhone,
        String tutorEmail,

        String studentName,
        String studentPhone,
        String studentEmail,
        String studentAddress,

        String subjectName,
        BigDecimal tuitionRate,
        String scheduleDetail,
        BigDecimal introductionFee, // Phí giao lớp dự kiến
        BigDecimal estimatedMonthlyTuition,
        Integer feePercentage,

        String classCode,
        String gradeLevel
) {}