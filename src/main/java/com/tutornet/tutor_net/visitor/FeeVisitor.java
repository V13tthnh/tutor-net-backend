package com.tutornet.tutor_net.visitor;

import com.tutornet.tutor_net.entity.ClassRequest;
import com.tutornet.tutor_net.entity.TutorProfile;

public interface FeeVisitor {
    void visit(TutorProfile tutor);
    void visit(ClassRequest classRequest);
}