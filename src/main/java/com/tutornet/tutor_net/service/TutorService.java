package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;

public interface TutorService {
    void processTutorInvitation(Long tutorId, Long studentUserId, InviteTutorRequest request);
    void acceptTutorInvitation(Long invitationId, Long tutorUserId);

}
