package com.tutornet.tutor_net.dto.request;

import jakarta.validation.constraints.*;

public class ReviewRequest {

    // Request đánh giá (Dùng cho cả tài khoản đăng nhập và Khách vãng lai qua Magic Link)
    public record SubmitReviewRequest(
            @NotNull(message = "Mã hợp đồng không được để trống")
            Long contractId,

            @Min(value = 1, message = "Đánh giá tối thiểu là 1 sao")
            @Max(value = 5, message = "Đánh giá tối đa là 5 sao")
            Integer rating,

            @Size(max = 2000, message = "Bình luận không được vượt quá 2000 ký tự")
            String comment,

            // Truyền token này lên nếu là Khách vãng lai làm khảo sát từ Email (Magic Link)
            String guestReviewToken
    ) {}
}
