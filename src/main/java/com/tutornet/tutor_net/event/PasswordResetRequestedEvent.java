package com.tutornet.tutor_net.event;

public record PasswordResetRequestedEvent(
        String email,
        String fullName,
        String resetToken
){}
