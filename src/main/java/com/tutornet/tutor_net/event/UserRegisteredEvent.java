package com.tutornet.tutor_net.event;

import com.tutornet.tutor_net.entity.User;

public record UserRegisteredEvent(
        User user,
        String verificationToken
) {
    public String email() { return user.getEmail();}
}
