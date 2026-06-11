package com.tutornet.tutor_net.controller;

import com.tutornet.tutor_net.dto.request.InviteTutorRequest;
import com.tutornet.tutor_net.dto.request.TutorSearchRequest.SearchFilter;
import com.tutornet.tutor_net.dto.response.ApiResponse;
import com.tutornet.tutor_net.dto.response.TutorSearchResponse.*;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.security.CustomUserDetails;
import com.tutornet.tutor_net.service.TutorSearchService;
import com.tutornet.tutor_net.util.PageableUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tutors")
@RequiredArgsConstructor
public class TutorSearchController {

    private final TutorSearchService tutorSearchService;

    /**
     * GET /api/v1/tutors
     *   ?keyword=Nguyễn
     *   &subjectIds=1,2
     *   &provinces=Hà Nội,TP.HCM
     *   &genders=MALE,FEMALE
     *   &teachingModes=ONLINE,OFFLINE
     *   &page=1&size=12
     *   &sortBy=ratingAvg&sortDir=desc
     */
    @GetMapping
    public ResponseEntity<ApiResponse<UserRoleResponse.PageResponse<TutorCardResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(required = false) List<String> provinces,
            @RequestParam(required = false) List<GenderType> genders,
            @RequestParam(required = false) List<TeachingMode> teachingModes,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "ratingAvg") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Pageable pageable = PageableUtils.build(page, size, null, sortBy, sortDir);
        SearchFilter filter = new SearchFilter(keyword, subjectIds, provinces, genders, teachingModes);
        return ResponseEntity.ok(ApiResponse.ok(tutorSearchService.search(filter, pageable)));
    }

    /**
     * GET /api/v1/tutors/filter-options
     * Trả về các lựa chọn lọc dựa trên data thực tế trong DB
     */
    @GetMapping("/filter-options")
    public ResponseEntity<ApiResponse<FilterOptionsResponse>> filterOptions() {
        return ResponseEntity.ok(ApiResponse.ok(tutorSearchService.getFilterOptions()));
    }
}