package com.tutornet.tutor_net.dto.response;

import lombok.Builder;

@Builder
public record PaymentResponse(
        String checkoutUrl,      // Link để redirect sang VNPay / PayOS
        String transactionCode   // Mã giao dịch để Frontend có thể theo dõi (Polling) nếu cần
) {}
