package com.tutornet.tutor_net.enums;

public enum UserStatus {
    ACTIVE("Đang hoạt động"),
    INACTIVE("Ngừng hoạt động"),
    SUSPENDED("Bị khóa"),
    PENDING_VERIFICATION("Chờ xác minh");

    private final String label;

    UserStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
