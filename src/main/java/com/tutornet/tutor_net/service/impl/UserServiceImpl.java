package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.UserRequest.*;
import com.tutornet.tutor_net.dto.response.UserFilterOptionsResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import com.tutornet.tutor_net.entity.Role;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.entity.UserRole;
import com.tutornet.tutor_net.enums.UserStatus;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.mapper.UserMapper;
import com.tutornet.tutor_net.repository.RoleRepository;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.repository.UserRoleRepository;
import com.tutornet.tutor_net.repository.spec.UserSpecification;
import com.tutornet.tutor_net.service.UserService;
import com.tutornet.tutor_net.util.AddressUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository         userRepository;
    private final RoleRepository         roleRepository;
    private final UserRoleRepository     userRoleRepository;
    private final PasswordEncoder        passwordEncoder;
    private final FileStorageServiceImpl fileStorageService;
    private final UserMapper userMapper;

    private static final List<Long> ADMIN_ROLE_IDS = List.of(1L, 2L);

    @Override
    public PageResponse<UserSummaryResponse> getUsers(String keyword, List<UserStatus> statuses,
                                                      List<String> roleSlugs, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecification.hasKeyword(keyword))
                .and(UserSpecification.hasStatuses(statuses))
                .and(UserSpecification.hasRoleSlugs(roleSlugs))
                .and(UserSpecification.hasAdminRoleIds(ADMIN_ROLE_IDS));

        Page<User> page = userRepository.findAll(spec, pageable);

        List<UserSummaryResponse> content = page.getContent().stream()
                .map(userMapper::toSummary)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    public UserDetailResponse getUserById(Long id) {
        User user = userRepository.findByIdWithRoles(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
        return userMapper.toDetail(user);
    }

    @Override
    public UserFilterOptionsResponse getFilterOptions() {
        List<UserFilterOptionsResponse.StatusOption> statuses =
                userRepository.findDistinctStatuses(ADMIN_ROLE_IDS)
                        .stream()
                        .map(s -> new UserFilterOptionsResponse.StatusOption(s.name(), toStatusLabel(s)))
                        .toList();

        List<UserFilterOptionsResponse.RoleOption> roles =
                userRepository.findAdminRoles(ADMIN_ROLE_IDS)
                        .stream()
                        .map(r -> new UserFilterOptionsResponse.RoleOption(r.getId(), r.getSlug(), r.getName()))
                        .toList();

        return new UserFilterOptionsResponse(statuses, roles);
    }

    public List<String> getStatuses() {
        return Arrays.stream(UserStatus.values()).map(Enum::name).toList();
    }

    @Override
    @Transactional
    public UserDetailResponse createAdmin(CreateAdminRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException("Email '" + request.email() + "' đã được sử dụng");
        }

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.roleIds()));
        if (roles.isEmpty()) {
            throw new BusinessException("Phải chọn ít nhất 1 role hợp lệ");
        }

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .fullName(request.fullName())
                .phone(request.phone())
                .gender(request.gender())
                .status(request.status())
                .isVerified(true)
                .loginCount(0)
                .build();

        for (Role role : roles) {
            user.getUserRoles().add(UserRole.builder().user(user).role(role).build());
        }

        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse updateAdmin(Long id, UpdateAdminRequest request) {
        User user = findUserOrThrow(id);

        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setGender(request.gender());
        user.setCurrentAddress(AddressUtils.build(request.address(), request.ward(), request.province()));

        if (request.status() != null) {
            user.setStatus(request.status());
        }

        // socialLinks: null = không thay đổi; empty map = xoá hết
        if (request.socialLinks() != null) {
            user.setSocialLinks(request.socialLinks());
        }

        if (request.password() != null && !request.password().isBlank()) {
            if (!request.password().equals(request.confirmPassword())) {
                throw new BusinessException("Xác nhận mật khẩu không khớp");
            }
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse updateUserProfile(Long id, UpdateProfileRequest request) {
        User user = findUserOrThrow(id);

        user.setFullName(request.fullName());
        user.setPhone(request.phone());
        user.setGender(request.gender());
        user.setBirthYear(request.birthYear());

        // Ghép 3 cấp địa chỉ thành 1 chuỗi lưu vào DB
        user.setCurrentAddress(AddressUtils.build(request.address(), request.ward(), request.province()));

        user.setHometownAddress(AddressUtils.build(
                request.hometownAddress(),
                request.hometownWard(),
                request.hometownProvince()
        ));

        // socialLinks: null = không thay đổi; empty map = xoá hết
        if (request.socialLinks() != null) {
            user.setSocialLinks(request.socialLinks());
        }

        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse updateStatus(Long id, UpdateStatusRequest request) {
        User user = findUserOrThrow(id);
        user.setStatus(request.status());
        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserDetailResponse updateAvatar(Long id, UpdateAvatarRequest request) {
        User user = findUserOrThrow(id);
        fileStorageService.deleteAvatar(user.getAvatarUrl());
        user.setAvatarUrl(request.avatarUrl());
        return userMapper.toDetail(userRepository.save(user));
    }

    @Override
    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        User user = findUserOrThrow(id);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new BusinessException("Mật khẩu mới không được trùng mật khẩu hiện tại");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = findUserOrThrow(id);
        boolean isSuperAdmin = user.getUserRoles().stream()
                .anyMatch(ur -> "super_admin".equals(ur.getRoleSlug()));
        if (isSuperAdmin) {
            throw new BusinessException("Không thể xoá tài khoản Super Admin");
        }
        userRepository.delete(user);
    }

    @Override
    @Transactional
    public UserDetailResponse assignRole(Long userId, AssignRoleRequest request, Long assignedById) {
        User user       = findUserOrThrow(userId);
        Role role       = roleRepository.findById(request.roleId())
                .orElseThrow(() -> ResourceNotFoundException.of("Role", request.roleId()));
        User assignedBy = userRepository.findById(assignedById)
                .orElseThrow(() -> ResourceNotFoundException.of("User (assignedBy)", assignedById));

        if (userRoleRepository.existsByUserIdAndRoleId(userId, request.roleId())) {
            throw new BusinessException("User đã có role '" + role.getName() + "'");
        }

        userRoleRepository.save(UserRole.builder()
                .user(user).role(role).assignedBy(assignedBy).build());

        return userMapper.toDetail(userRepository.findByIdWithRoles(userId).orElseThrow());
    }

    @Override
    @Transactional
    public UserDetailResponse revokeRole(Long userId, Long roleId) {
        findUserOrThrow(userId);
        int deleted = userRoleRepository.deleteByUserIdAndRoleId(userId, roleId);
        if (deleted == 0) {
            throw new BusinessException("User không có role này để gỡ");
        }
        return userMapper.toDetail(userRepository.findByIdWithRoles(userId).orElseThrow());
    }


    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("User", id));
    }

    private String toStatusLabel(UserStatus status) {
        return switch (status) {
            case ACTIVE               -> "Hoạt động";
            case INACTIVE             -> "Không hoạt động";
            case SUSPENDED            -> "Đã khóa";
            case PENDING_VERIFICATION -> "Chờ xác thực";
        };
    }

//    /** Entity → UserSummaryResponse (danh sách, payload nhỏ) */
//    private UserSummaryResponse toSummary(User u) {
//        List<RoleSummary> roles = u.getUserRoles().stream()
//                .map(ur -> new RoleSummary(ur.getRole().getId(), ur.getRole().getName(), ur.getRole().getSlug()))
//                .toList();
//        return new UserSummaryResponse(
//                u.getId(), u.getEmail(), u.getFullName(), u.getPhone(),
//                u.getAvatarUrl(), u.getStatus(), u.getIsVerified(),
//                u.getCreatedAt(), roles
//        );
//    }
//
//    /**
//     * Entity → UserDetailResponse (chi tiết / sau mọi thao tác ghi).
//     *
//     * Địa chỉ: parse chuỗi address từ DB thành 3 field riêng bằng AddressUtils.parse()
//     * để frontend không cần tự tách.
//     */
//    private UserDetailResponse toDetail(User u) {
//        String avatarUrl = fileStorageService.toFullUrl(u.getAvatarUrl());
//
//        // Tách chuỗi address → province / ward / address
//        AddressUtils.Parts addr = AddressUtils.parse(u.getCurrentAddress());
//
//        List<UserRoleDetail> roles = u.getUserRoles().stream()
//                .map(ur -> new UserRoleDetail(
//                        ur.getId(),
//                        new RoleSummary(ur.getRole().getId(), ur.getRole().getName(), ur.getRole().getSlug()),
//                        ur.getAssignedBy() != null ? ur.getAssignedBy().getFullName() : null,
//                        ur.getExpiresAt(),
//                        ur.getCreatedAt()
//                ))
//                .toList();
//
//        return new UserDetailResponse(
//                u.getId(),
//                u.getEmail(),
//                u.getFullName(),
//                u.getPhone(),
//                avatarUrl,
//                u.getGender(),
//                addr.province(),
//                addr.ward(),
//                addr.address(),
//                u.getSocialLinks(),
//                u.getStatus(),
//                u.getIsVerified(),
//                u.getEmailVerifiedAt(),
//                u.getLastLoginAt(),
//                u.getLoginCount(),
//                u.getCreatedAt(),
//                u.getUpdatedAt(),
//                roles
//        );
//    }
}