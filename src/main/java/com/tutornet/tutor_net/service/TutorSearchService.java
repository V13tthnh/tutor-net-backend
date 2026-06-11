package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.TutorSearchRequest.SearchFilter;
import com.tutornet.tutor_net.dto.response.TutorSearchResponse.*;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import org.springframework.data.domain.Pageable;

public interface TutorSearchService {
    UserRoleResponse.PageResponse<TutorCardResponse> search(SearchFilter filter, Pageable pageable);
    FilterOptionsResponse getFilterOptions();
}
