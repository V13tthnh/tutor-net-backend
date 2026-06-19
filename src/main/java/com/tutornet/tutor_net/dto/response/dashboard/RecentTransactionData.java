package com.tutornet.tutor_net.dto.response.dashboard;

import java.math.BigDecimal;
import java.time.Instant;

public record RecentTransactionData(
        Long           id,
        String         transactionCode,    // "TXN-260617-XYZ"
        String         tutorName,          // users.full_name của người thanh toán
        String         contractNumber,     // contracts.contract_number
        BigDecimal amount,
        String         paymentMethod,      // "VNPAY" | "PAYOS" | "BANK_TRANSFER"
        String         status,             // "SUCCESS" | "PENDING" | ...
        Instant paidAt
){}
