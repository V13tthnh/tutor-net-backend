package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
import com.tutornet.tutor_net.dto.response.TutorResponse;

public interface TutorService {
    void processTutorInvitation(Long tutorId, Long studentUserId, InviteTutorRequest request);
    void acceptTutorInvitation(Long invitationId, Long tutorUserId);
    TutorResponse.TutorProfileResponse getTutorById(Long tutorId);
}
