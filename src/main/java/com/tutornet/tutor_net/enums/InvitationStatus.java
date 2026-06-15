package com.tutornet.tutor_net.enums;

public enum InvitationStatus {
    PENDING,    // Chờ gia sư xác nhận
    ACCEPTED,   // Gia sư đã đồng ý
    REJECTED,   // Gia sư đã từ chối
    CANCELED_BY_ADMIN,
    EXPIRED     // Hết hạn
}
