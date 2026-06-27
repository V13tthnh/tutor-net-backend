package com.tutornet.tutor_net.mail.payload;

public record StudentTutorPayload(
        String studentName,
        String tutorName,
        String extraMessage,
        Boolean isGuest,
        String actionUrl
) {
    public StudentTutorPayload(String studentName, String tutorName, String extraMessage) {
        this(studentName, tutorName, extraMessage, false, "http://localhost:3000/account/my-classes");
    }
}
