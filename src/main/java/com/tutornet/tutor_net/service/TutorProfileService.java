package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.TutorRequest.*;
import com.tutornet.tutor_net.dto.request.UserRequest;
import com.tutornet.tutor_net.dto.response.TutorResponse.*;
import com.tutornet.tutor_net.dto.response.*;

import java.util.List;

public interface TutorProfileService {
    TutorProfileResponse createProfile(Long userId, TutorProfileRequest request);

    TutorProfileResponse getMyProfile(Long userId);

    TutorProfileResponse updateProfile(Long userId, TutorProfileRequest request);

    TutorSubjectResponse addSubject(Long userId, TutorSubjectRequest request);

    void removeSubject(Long userId, Long subjectId);

    void removeCertificate(Long userId, Long certificateId);

    CertificateResponse addCertificate(Long userId, String name, String fileUrl);

    List<AvailabilityResponse> replaceAvailability(
            Long userId, List<AvailabilityRequest> requests);


    TutorProfileResponse resubmitProfile(Long tutorUserId, TutorProfileRequest request);

    TutorProfileResponse submitForReview(Long userId);
}
