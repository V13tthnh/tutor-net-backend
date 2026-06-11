package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.StudentInitiativeLvl;
import com.tutornet.tutor_net.enums.StudentProgressLvl;
import java.time.LocalDateTime;
import java.util.List;

public record StudyReportResponse(
        Long id,
        Long contractId,
        String contractNumber,
        Long tutorId,
        String tutorName,
        Integer reportMonth,
        Integer reportYear,

//        List<SessionDetail> sessionDetails, // Trả về mảng objects để React render ra bảng

        StudentProgressLvl studentProgress,
        StudentInitiativeLvl studentInitiative,
        String improvementPoints,
        String weakPoints,
        String nextMonthPlan,
        String suggestionToParent,

        LocalDateTime emailSentAt, // Báo cho gia sư biết email đã được gửi thành công chưa
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}