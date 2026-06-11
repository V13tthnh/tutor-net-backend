package com.tutornet.tutor_net.enums;

public enum GenderType {
    MALE("Nam"),
    FEMALE("Nữ"),
    OTHER("Khác");

    private final String label;

    GenderType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
