package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ReviewCreateRequest(
        @NotNull(message = "ID hợp đồng không được để trống")
        Long contractId,

        @NotNull(message = "Vui lòng chọn số sao đánh giá")
        @Min(value = 1, message = "Đánh giá thấp nhất là 1 sao")
        @Max(value = 5, message = "Đánh giá cao nhất là 5 sao")
        Integer rating,

        String comment,

        // Nhận token từ URL nếu học viên click từ Email (Magic Link)
        String guestReviewToken
) {}