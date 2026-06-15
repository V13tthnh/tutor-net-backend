package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.ClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.CreateClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.ReviewClassRequest;
import com.tutornet.tutor_net.dto.request.ClassRequest.BulkReviewClassRequest;
import com.tutornet.tutor_net.dto.response.ClassRequestDropdownResponse;
import com.tutornet.tutor_net.dto.response.ClassRequestFilterOptionsResponse;
import com.tutornet.tutor_net.dto.response.ClassRequestResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ClassRequestService {
    /**
     * Tiếp nhận yêu cầu đăng lớp từ Học viên / Phụ huynh
     * @param request Dữ liệu form đăng ký lớp
     * @param authenticatedUserId ID của user nếu đã đăng nhập, truyền null nếu là khách vãng lai (Guest)
     */
    ClassRequestResponse createClassRequest(CreateClassRequest request, Long authenticatedUserId);

    UserRoleResponse.PageResponse<ClassRequestResponse> getJobBoardRequests(Long tutorUserId, Long subjectId, String teachingMode, Pageable pageable);

    List<ClassRequestResponse> createBulkClassRequests(List<CreateClassRequest> requests, Long userId);

    UserRoleResponse.PageResponse<ClassRequestResponse> getAllRequestsForAdmin(
            String keyword, String statusStr, Long subjectId, String teachingModeStr, Pageable pageable);

    ClassRequestResponse getRequestDetailForAdmin(Long id);

    ClassRequestResponse reviewClassRequest(Long classRequestId,
                                            ReviewClassRequest reviewRequest,
                                            Long adminId);

    ClassRequestFilterOptionsResponse getClassRequestFilterOptions();

    List<ClassRequestResponse> reviewBulkClassRequests(BulkReviewClassRequest bulkRequest, Long adminId);

    ClassRequestResponse trackClassRequest(ClassRequest.TrackClassRequest request, String clientIp);

    List<ClassRequestDropdownResponse> getMyActiveRequestsForDropdown(Long userId);

}
