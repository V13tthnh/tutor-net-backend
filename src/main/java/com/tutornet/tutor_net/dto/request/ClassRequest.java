package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.ClassRequestStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public class ClassRequest {

    // Request tạo lớp học mới (Dùng chung cho cả Khách vãng lai và User đã đăng nhập)
    public record CreateClassRequest(
            @NotBlank(message = "Tên liên hệ không được để trống")
            @Size(max = 100, message = "Tên không được vượt quá 100 ký tự")
            String contactName,

            @NotBlank(message = "Số điện thoại không được để trống")
            @Pattern(regexp = "^(0|\\+84)(3[2-9]|5[6|8|9]|7[0|6-9]|8[1-9]|9[0-9])[0-9]{7}$",
                    message = "Số điện thoại không hợp lệ")
            String contactPhone,

            @Email(message = "Email không đúng định dạng")
            @Size(max = 255, message = "Email không được vượt quá 255 ký tự")
            String contactEmail,

            @NotNull(message = "Vui lòng chọn môn học")
            Long subjectId,

            @NotBlank(message = "Vui lòng nhập khối lớp/trình độ")
            @Size(max = 50, message = "Khối lớp không được vượt quá 50 ký tự")
            String gradeLevel,

            @DecimalMin(value = "0.0", message = "Học phí đề xuất không được là số âm")
            BigDecimal proposedPrice,

            @Min(1) @Max(7)
            Integer sessionsPerWeek,

            @Min(30) @Max(300)
            Integer durationMinutes,

            @NotNull(message = "Vui lòng chọn hình thức học")
            String teachingMode, // ONLINE, OFFLINE, HYBRID

            String addressDetail, // Bắt buộc nếu chọn OFFLINE ở tầng Service validate
            String studentNotes,

            Long targetTutorId // Điền ID nếu muốn mời đích danh gia sư, để trống nếu đăng công khai
    ) {}

    public record TrackClassRequest(
            @NotBlank(message = "Vui lòng nhập mã lớp học")
            String classCode,

            @NotBlank(message = "Vui lòng nhập số điện thoại để xác thực")
            String contactPhone
    ) {}

    public record BulkClassRequest(
            @NotEmpty(message = "Danh sách không được để trống")
            @Valid
            List<CreateClassRequest> requests
    ) {}

    public record ReviewClassRequest(
            @NotNull(message = "Trạng thái không được để trống")
            ClassRequestStatus status,

            String rejectionReason
    ) {}

    public record BulkReviewClassRequest(
            @NotEmpty(message = "Danh sách ID lớp học không được để trống")
            List<Long> ids,

            @NotNull(message = "Trạng thái phê duyệt không được để trống")
            ClassRequestStatus status,

            String rejectionReason
    ) {}
}
