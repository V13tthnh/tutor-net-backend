package com.tutornet.tutor_net.mail.payload;

public record PasswordResetPayload(String fullName, String token) {}
