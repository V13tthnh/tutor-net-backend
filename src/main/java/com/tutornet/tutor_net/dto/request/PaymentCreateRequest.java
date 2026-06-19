package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;

public record PaymentCreateRequest(
        @NotNull(message = "ID hợp đồng không được để trống")
        Long contractId,

        @NotNull(message = "Phương thức thanh toán không được để trống")
        PaymentMethod paymentMethod
) {}
