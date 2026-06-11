package com.tutornet.tutor_net.enums;

public enum TutorStatus {
    DRAFT("Nháp"),
    PENDING_REVIEW("Chờ duyệt"),
    APPROVED("Đã duyệt"),
    REJECTED("Đã từ chối"),
    SUSPENDED("Tạm dừng");

    private final String label;

    TutorStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
