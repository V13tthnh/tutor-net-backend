package com.tutornet.tutor_net.visitor;

import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorProfile;

public interface FeeElement {
    void accept(FeeVisitor visitor);
}