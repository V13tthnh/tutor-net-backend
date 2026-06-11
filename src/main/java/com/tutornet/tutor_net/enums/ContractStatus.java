package com.tutornet.tutor_net.enums;

public enum ContractStatus {
    DRAFT,              // Bản nháp hệ thống vừa tạo
    PENDING_SIGNATURE,  // Chờ gia sư click xác nhận ký
    ACTIVE,             // Gia sư đã ký, Lớp đang diễn ra (Chưa đóng phí môi giới)
    COMPLETED,          // Thành công: Lớp ổn định & Gia sư ĐÃ ĐÓNG PHÍ xong
    CANCELLED,          // Hủy bỏ (Lớp hỏng trong thời gian dạy thử)
    VIOLATED            // Vi phạm: Gia sư dạy tiếp nhưng "bùng" phí (Quá 35 ngày không đóng)
}