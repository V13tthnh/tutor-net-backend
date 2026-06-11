package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.ApplicationRequest.ApplyClassRequest;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;

public interface ClassApplicationService {
    ClassApplicationResponse respondToDirectInvite(Long requestId, boolean isAccepted, String message, Long tutorUserId);
    ClassApplicationResponse applyForClass(ApplyClassRequest request, Long tutorUserId);
}
