package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.ApplicationRequest.ApplyClassRequest;
import com.tutornet.tutor_net.dto.response.ClassApplicationResponse;
import com.tutornet.tutor_net.security.CustomUserDetails;

import java.util.List;

public interface ClassApplicationService {
    ClassApplicationResponse respondToDirectInvite(Long requestId, boolean isAccepted, String message, Long tutorUserId);
    ClassApplicationResponse applyForClass(Long classRequestId, ApplyClassRequest request, Long tutorUserId);
    List<ClassApplicationResponse> getApplicationsForClass(Long classRequestId, CustomUserDetails currentUser);
    List<ClassApplicationResponse> getApplicationsForAdmin(Long classRequestId);
    ClassApplicationResponse acceptApplication(Long classRequestId, Long applicationId, Long studentUserId);
    ClassApplicationResponse hideApplication(Long classRequestId, Long applicationId);
}
