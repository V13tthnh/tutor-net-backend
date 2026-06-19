package com.tutornet.tutor_net.dto.response;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long contractId,
        Long tutorId,
        String reviewerName, // "Ẩn danh" hoặc tên thật nếu có tài khoản hệ thống
        Integer rating,
        String comment,
        Boolean isPublic,
        Instant createdAt
) {}