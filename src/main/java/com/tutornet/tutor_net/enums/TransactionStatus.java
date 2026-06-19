package com.tutornet.tutor_net.enums;

public enum TransactionStatus {
    PENDING,    // Vừa tạo mã thanh toán, đang chờ quét QR/chuyển tiền
    SUCCESS,    // Thanh toán thành công, tiền đã vào tài khoản
    FAILED,     // Giao dịch thất bại (sai mã OTP, không đủ tiền...)
    CANCELLED,  // Người dùng tự hủy giao dịch
    REFUNDED    // Đã hoàn tiền lại cho gia sư (do lớp hỏng)
}
