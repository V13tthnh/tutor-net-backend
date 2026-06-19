package com.tutornet.tutor_net.dto.response;

import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
import java.math.BigDecimal;
import java.time.Instant;

public record ClassRequestResponse(
        Long id,
        String classCode,
        Long userId, // NULL nếu là khách vãng lai
        String contactName,
        String contactPhone, // Thường Admin mới cấu hình thấy trường này, UI ngoài sẽ che ẩn
        String contactEmail,
        Long subjectId,
        String subjectName,
        String gradeLevel,
        BigDecimal proposedPrice,
        BigDecimal hourlyRate,
        Integer sessionsPerWeek,
        Integer durationMinutes,
        TeachingMode teachingMode,
        String province,
        String ward,
        String address,
        String studentNotes,
        Long targetTutorId,
        String targetTutorName,
        ClassRequestStatus status,
        String rejectionReason,     // NULL nếu chưa bị từ chối — Admin điền khi REJECTED
        Integer totalApplicants, // Số lượng gia sư đã gửi yêu cầu nhận lớp này (Đáp ứng đúng logic đếm)
        Instant createdAt,
        Instant updatedAt,
        boolean hasAccount
) {}

