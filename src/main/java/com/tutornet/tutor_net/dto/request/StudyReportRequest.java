package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.StudentInitiativeLvl;
import com.tutornet.tutor_net.enums.StudentProgressLvl;
import jakarta.validation.constraints.*;

public class StudyReportRequest {

    // Gia sư tạo hoặc cập nhật báo cáo tháng
    public record CreateOrUpdateReportRequest(
            @NotNull(message = "Mã hợp đồng không được để trống")
            Long contractId,

            @Min(value = 1, message = "Tháng phải từ 1 đến 12")
            @Max(value = 12, message = "Tháng phải từ 1 đến 12")
            Integer reportMonth,

            @Min(value = 2020, message = "Năm không hợp lệ")
            Integer reportYear,

//            @NotEmpty(message = "Danh sách chi tiết buổi học không được để trống")
//            List<SessionDetail> sessionDetails, // Frontend sẽ truyền lên mảng objects

            @NotNull(message = "Vui lòng đánh giá mức độ tiếp thu")
            StudentProgressLvl studentProgress,

            @NotNull(message = "Vui lòng đánh giá tính chủ động")
            StudentInitiativeLvl studentInitiative,

            @Size(max = 2000, message = "Điểm tiến bộ không vượt quá 2000 ký tự")
            String improvementPoints,

            @Size(max = 2000, message = "Điểm cần cải thiện không vượt quá 2000 ký tự")
            String weakPoints,

            @Size(max = 2000, message = "Kế hoạch tháng sau không vượt quá 2000 ký tự")
            String nextMonthPlan,

            @Size(max = 2000, message = "Đề xuất với phụ huynh không vượt quá 2000 ký tự")
            String suggestionToParent
    ) {}
}
