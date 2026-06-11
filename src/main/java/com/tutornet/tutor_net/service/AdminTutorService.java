package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.TutorRequest;
import com.tutornet.tutor_net.dto.response.TutorResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.enums.TutorStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminTutorService {
    UserRoleResponse.PageResponse<TutorResponse.TutorSummaryResponse> list(
            String keyword,
            List<TutorStatus> statuses,
            List<Long> subjectIds,
            Pageable pageable
    );

    TutorResponse.TutorProfileResponse getTutorById(Long tutorId);      // xem CV đầy đủ

    TutorResponse.TutorStatsResponse getStats();

    TutorResponse.TutorProfileResponse reviewTutor(Long tutorId, TutorRequest.ReviewTutorRequest request, Long adminId);

    void suspendTutor(Long tutorId, String reason);

    void unsuspendTutor(Long tutorId);

    TutorResponse.TutorFilterOptionsResponse getFilterOptions();
}
