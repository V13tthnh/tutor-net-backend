package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.EduLevel;
import com.tutornet.tutor_net.enums.ProficiencyLevel;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.enums.TutorStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalTime;

public final class TutorRequest {

    public record TutorProfileRequest(
            @Size(max = 255, message = "Tiêu đề không được vượt quá 255 ký tự")
            String headline,

            @Size(max = 5000, message = "Giới thiệu bản thân không được vượt quá 5000 ký tự")
            String bio,

            @Min(value = 0, message = "Số năm kinh nghiệm không được là số âm")
            @Max(value = 50, message = "Số năm kinh nghiệm không hợp lệ")
            Integer experienceYears,

            EduLevel educationLevel,

            Boolean isAvailable,

            @NotNull(message = "Vui lòng chọn hình thức giảng dạy")
            TeachingMode teachingMode,

            @Size(max = 50, message = "Nghề nghiệp không được vượt quá 50 ký tự")
            String occupation,

            @Min(value = 1, message = "Năm sinh viên phải từ 1 trở lên")
            @Max(value = 6, message = "Năm sinh viên không hợp lệ (tối đa 6)")
            Integer studentYear,

            @Size(max = 200, message = "Chuyên ngành không được vượt quá 200 ký tự")
            String major,

            @Size(max = 255, message = "Tên trường không được vượt quá 255 ký tự")
            String university,

            @Min(value = 1950, message = "Năm tốt nghiệp không hợp lệ")
            @Max(value = 2100, message = "Năm tốt nghiệp không hợp lệ")
            Integer graduationYear,

            @Size(max = 5000, message = "Thành tích không được vượt quá 5000 ký tự")
            String achievements,

            @Pattern(
                    regexp = "^$|^(https?://.*|/.*)\\.(jpg|jpeg|png|gif|webp|svg)(\\?[\\w=&%.\\-]*)?$",
                    message = "Ảnh đại diện phải là đường dẫn ảnh hợp lệ (.jpg, .png, .webp,...)"
            )
            String avatarUrl,

            @Pattern(
                    regexp = "^$|^(https?://.*|/.*)\\.(jpg|jpeg|png|gif|webp|svg)(\\?[\\w=&%.\\-]*)?$",
                    message = "Ảnh thẻ sinh viên phải là đường dẫn ảnh hợp lệ (.jpg, .png, .webp,...)"
            )
            String idCardFrontUrl,

            String teachingProvince,
            String teachingWard
    ) {}

    // Request: Thêm môn dạy
    public record TutorSubjectRequest(
            @NotNull(message = "Môn học không được để trống")
            Long subjectId,

            @NotNull(message = "Trình độ chuyên môn không được để trống")
            ProficiencyLevel proficiencyLevel,

            @NotNull(message = "Học phí không được để trống")
            @DecimalMin(value = "0.0", message = "Học phí không được là số âm")
            BigDecimal hourlyRate
    ) {}

    // Request: Thêm lịch rảnh
    public record AvailabilityRequest(
            @Min(value = 0, message = "Thứ trong tuần phải từ 0 (Chủ nhật) đến 6 (Thứ 7)")
            @Max(value = 6, message = "Thứ trong tuần phải từ 0 (Chủ nhật) đến 6 (Thứ 7)")
            int dayOfWeek,

            @NotNull(message = "Thời gian bắt đầu không được để trống")
            LocalTime startTime,

            @NotNull(message = "Thời gian kết thúc không được để trống")
            LocalTime endTime
    ) {}

    public record CertificateRequest(
            @NotNull(message = "Vui lòng nhập tên chứng chỉ")
            String name,

            @Pattern(
                    regexp = "^$|^(https?://.*|/.*)\\.(jpg|jpeg|png|gif|webp|svg)(\\?[\\w=&%.\\-]*)?$",
                    message = "Chứng chỉ phải là đường dẫn ảnh hợp lệ (.jpg, .png, .webp,...)"
            )
            String fileUrl,

            Boolean isVerified
    ) {}

    // Duyệt / từ chối
    public record ReviewTutorRequest(
            @NotNull TutorStatus status,

            @Size(max = 1000)
            String rejectionReason
    ) {}
}