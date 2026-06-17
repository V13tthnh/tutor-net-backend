package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.ContractStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ContractDisputeRequest(
        @NotNull(message = "Trạng thái mới không được để trống")
        ContractStatus status, // Chỉ chấp nhận CANCELLED hoặc VIOLATED

        @NotBlank(message = "Vui lòng nhập lý do xử lý sự cố hợp đồng")
        String reason,

        boolean refundFee // Quyết định có hoàn lại tiền phí nhận lớp cho gia sư hay không
) {}