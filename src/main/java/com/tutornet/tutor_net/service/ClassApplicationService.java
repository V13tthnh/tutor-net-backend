package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.ApplicationRequest.ApplyClassRequest;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;

import java.util.List;

public interface ClassApplicationService {
    ClassApplicationResponse respondToDirectInvite(Long requestId, boolean isAccepted, String message, Long tutorUserId);
    ClassApplicationResponse applyForClass(Long classRequestId, ApplyClassRequest request, Long tutorUserId);
    List<ClassApplicationResponse> getApplicationsForClass(Long classRequestId, Long studentUserId);
    ClassApplicationResponse acceptApplication(Long classRequestId, Long applicationId, Long studentUserId);
}
