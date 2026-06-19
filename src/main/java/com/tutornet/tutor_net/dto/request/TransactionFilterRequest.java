package com.tutornet.tutor_net.dto.request;

import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Tham số lọc danh sách giao dịch cho Admin.
 * Tất cả đều optional — không truyền = lấy tất cả.
 */
public record TransactionFilterRequest(

        // Lọc theo trạng thái: PENDING | SUCCESS | FAILED | REFUNDED
        TransactionStatus status,

        // Lọc theo phương thức thanh toán: VNPAY | PAYOS | BANK_TRANSFER
        PaymentMethod paymentMethod,

        // Tìm kiếm tự do: mã giao dịch, mã HĐ, tên gia sư
        String search,

        // Lọc theo khoảng thời gian (createdAt)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate fromDate,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate toDate,

        // Phân trang
        int page,
        int size
) {
    // Compact constructor — gán default nếu null
    public TransactionFilterRequest {
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 100) size = 100;
    }
}