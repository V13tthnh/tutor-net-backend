package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.ContractStatus;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public class ContractRequest {

    // Admin khởi tạo hợp đồng khi chốt lớp
    public record CreateContractRequest(
            @NotNull(message = "Mã yêu cầu lớp học không được để trống")
            Long requestId,

            @NotNull(message = "Mã gia sư không được để trống")
            Long tutorId,

            @NotNull(message = "Phí giao lớp không được để trống")
            @DecimalMin(value = "0.0", message = "Phí giao lớp không được âm")
            BigDecimal introductionFee,

            @Min(value = 0, message = "Số buổi dạy thử tối thiểu là 0")
            Integer freeTrialCount
    ) {}

    // Gia sư upload file ảnh/PDF hợp đồng đã ký tay lên hệ thống
    public record UploadSignedContractRequest(
            @NotBlank(message = "Đường dẫn file hợp đồng không được để trống")
            String contractFileUrl
    ) {}

    // Admin cập nhật trạng thái đóng tiền hoặc hủy hợp đồng
    public record UpdateContractStatusRequest(
            @NotNull(message = "Trạng thái hợp đồng không được để trống")
            ContractStatus status
    ) {}
}