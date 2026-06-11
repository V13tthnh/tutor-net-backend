package com.tutornet.tutor_net.mapper;

import com.tutornet.tutor_net.dto.response.UserRoleResponse.RoleSummary;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.UserDetailResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.UserRoleDetail;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.UserSummaryResponse;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.service.impl.FileStorageServiceImpl;
import com.tutornet.tutor_net.util.AddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class UserMapper {

    private final FileStorageServiceImpl fileStorageService;

    /** * Chuyển đổi Entity -> UserSummaryResponse (Dùng cho danh sách phân trang)
     */
    public UserSummaryResponse toSummary(User u) {
        List<RoleSummary> roles = u.getUserRoles().stream()
                .map(ur -> new RoleSummary(ur.getRole().getId(), ur.getRole().getName(), ur.getRole().getSlug()))
                .toList();

        return new UserSummaryResponse(
                u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
                u.getAvatarUrl(), u.getStatus(), u.getIsVerified(),
                u.getCreatedAt(), roles
        );
    }

    /**
     * Chuyển đổi Entity -> UserDetailResponse (Dùng cho xem chi tiết / sau khi update)
     */
    public UserDetailResponse toDetail(User u) {
        String avatarUrl = fileStorageService.toFullUrl(u.getAvatarUrl());

        // Tách chuỗi address → province / ward / address
        AddressUtils.Parts currentAddr = AddressUtils.parse(u.getCurrentAddress());
        AddressUtils.Parts hometownAddr = AddressUtils.parse(u.getHometownAddress());

        List<UserRoleDetail> roles = u.getUserRoles().stream()
                .map(ur -> new UserRoleDetail(
                        ur.getId(),
                        new RoleSummary(ur.getRole().getId(), ur.getRole().getName(), ur.getRole().getSlug()),
                        ur.getAssignedBy() != null ? ur.getAssignedBy().getFullName() : null,
                        ur.getExpiresAt(),
                        ur.getCreatedAt()
                ))
                .toList();

        return new UserDetailResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                avatarUrl,
                u.getGender(),
                u.getBirthYear(),

                currentAddr.province(),
                currentAddr.ward(),
                currentAddr.address(),

                hometownAddr.province(),
                hometownAddr.ward(),
                hometownAddr.address(),

                u.getSocialLinks(),
                u.getStatus(),
                u.getIsVerified(),
                u.getEmailVerifiedAt(),
                u.getLastLoginAt(),
                u.getLoginCount(),
                u.getCreatedAt(),
                u.getUpdatedAt(),
                roles
        );
    }
}
