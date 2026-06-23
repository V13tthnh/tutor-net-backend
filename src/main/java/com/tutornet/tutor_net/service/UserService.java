package com.tutornet.tutor_net.service;

import com.tutornet.tutor_net.dto.request.UserRequest.*;
import com.tutornet.tutor_net.dto.response.UserFilterOptionsResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import com.tutornet.tutor_net.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UserService {

    PageResponse<UserSummaryResponse> getUsers(String keyword, List<UserStatus> statuses, List<String> roleSlugs, Pageable pageable);

    UserDetailResponse getUserById(Long id);

    UserDetailResponse updateStatus(Long id, UpdateStatusRequest request);

    UserDetailResponse createAdmin(CreateAdminRequest request);

    UserDetailResponse updateAdmin(Long id, UpdateAdminRequest request);

    @Transactional
    UserDetailResponse updateAvatar(Long id, UpdateAvatarRequest request);

    UserDetailResponse updateUserProfile(Long id, UpdateProfileRequest request);

    void resetPassword(Long id, ResetPasswordRequest request);

    void deleteUser(Long id);

    void deleteSelfAccount(Long id, DeleteAccountRequest request);

    UserDetailResponse assignRole(Long userId, AssignRoleRequest request, Long assignedById);

    UserDetailResponse revokeRole(Long userId, Long roleId);

    UserFilterOptionsResponse getFilterOptions();
}

