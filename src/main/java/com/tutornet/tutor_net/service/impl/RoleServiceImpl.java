package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.AssignUserRoleRequest;
import com.tutornet.tutor_net.dto.request.RolePermissionRequest.*;
import com.tutornet.tutor_net.entity.Permission;
import com.tutornet.tutor_net.entity.Role;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.entity.UserRole;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.repository.PermissionRepository;
import com.tutornet.tutor_net.repository.RoleRepository;
import com.tutornet.tutor_net.repository.UserRepository;
import com.tutornet.tutor_net.repository.spec.RoleSpecification;
import com.tutornet.tutor_net.service.RoleService;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository       roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository       userRepository;

    @Override
    public PageResponse<RoleResponse> getRoles(String keyword, Boolean isSystem, Pageable pageable) {
        Specification<Role> spec = Specification
                .where(RoleSpecification.hasKeyword(keyword))
                .and(RoleSpecification.isSystem(isSystem));

        Page<Role> page = roleRepository.findAll(spec, pageable);

        if (page.isEmpty()) {
            return PageResponse.empty(pageable);
        }

        List<Long> roleIds = page.getContent().stream()
                .map(Role::getId)
                .toList();

        Map<Long, Set<PermissionSummary>> permMap = roleRepository
                .findWithPermissionsByIds(roleIds)
                .stream()
                .collect(Collectors.toMap(
                        Role::getId,
                        r -> r.getPermissions().stream()
                                .map(this::toPermissionSummary)
                                .collect(Collectors.toSet())
                ));

        Map<Long, Long> userCountMap = roleRepository
                .countUsersByRoleIds(roleIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> ((Number) row[1]).longValue()
                ));

        List<RoleResponse> content = page.getContent().stream()
                .map(r -> toRoleResponse(
                        r,
                        permMap.getOrDefault(r.getId(), Set.of()),
                        userCountMap.getOrDefault(r.getId(), 0L)
                ))
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findWithPermissionsByIds(List.of(id))
                .stream().findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));

        long userCount = roleRepository.countUsersByRoleId(id);

        Set<PermissionSummary> permissions = role.getPermissions().stream()
                .map(this::toPermissionSummary)
                .collect(Collectors.toSet());

        return toRoleResponse(role, permissions, userCount);
    }

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        if (roleRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Slug '" + request.slug() + "' đã tồn tại");
        }
        if (roleRepository.existsByName(request.name())) {
            throw new BusinessException("Tên role '" + request.name() + "' đã tồn tại");
        }
        Role role = Role.builder()
                .name(request.name())
                .slug(request.slug())
                .description(request.description())
                .isSystem(false)
                .build();
        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = findOrThrow(id);
        role.setName(request.name());
        role.setDescription(request.description());
        return toResponse(roleRepository.save(role));
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = findOrThrow(id);
        if (Boolean.TRUE.equals(role.getIsSystem())) {
            throw new BusinessException("Không thể xoá system role '" + role.getName() + "'");
        }
        long userCount = roleRepository.countUsersByRoleId(id);
        if (userCount > 0) {
            throw new BusinessException(
                    "Role đang được gán cho " + userCount + " user. Gỡ hết trước khi xoá."
            );
        }
        roleRepository.delete(role);
    }

    // ─── Role ↔ Permission ────────────────────────────────────────────────────

    @Override
    @Transactional
    public RoleResponse syncPermissions(Long roleId, SyncPermissionsRequest request) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleId));

        Set<Permission> newPermissions = new HashSet<>(
                permissionRepository.findAllById(request.permissionIds())
        );
        if (newPermissions.size() != request.permissionIds().size()) {
            throw new BusinessException("Có một hoặc nhiều permissionId không tồn tại");
        }

        role.getPermissions().clear();
        role.getPermissions().addAll(newPermissions);

        long userCount = roleRepository.countUsersByRoleId(roleId);
        return toResponse(roleRepository.save(role), userCount);
    }

    @Override
    @Transactional
    public RoleResponse addPermission(Long roleId, TogglePermissionRequest request) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleId));
        Permission permission = permissionRepository.findById(request.permissionId())
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", request.permissionId()));

        boolean added = role.getPermissions().add(permission);
        if (!added) {
            throw new BusinessException("Role đã có permission '" + permission.getName() + "'");
        }

        long userCount = roleRepository.countUsersByRoleId(roleId);
        return toResponse(roleRepository.save(role), userCount);
    }

    @Override
    @Transactional
    public RoleResponse removePermission(Long roleId, Long permissionId) {
        Role role = roleRepository.findByIdWithPermissions(roleId)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleId));

        boolean removed = role.getPermissions()
                .removeIf(p -> p.getId().equals(permissionId));
        if (!removed) {
            throw new BusinessException("Role không có permission này để gỡ");
        }

        long userCount = roleRepository.countUsersByRoleId(roleId);
        return toResponse(roleRepository.save(role), userCount);
    }

    // ─── Role ↔ User ──────────────────────────────────────────────────────────

    @Override
    public PageResponse<UserSummaryResponse> getUsersByRole(Long roleId, Pageable pageable) {
        if (!roleRepository.existsById(roleId)) {
            throw ResourceNotFoundException.of("Role", roleId);
        }

        Page<User> page = userRepository.findByRoleId(roleId, pageable);

        if (page.isEmpty()) {
            return PageResponse.empty(pageable);
        }

        List<UserSummaryResponse> content = page.getContent().stream()
                .map(this::toUserSummaryResponse)
                .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    @Override
    @Transactional
    public void assignUserToRole(Long roleId, AssignUserRoleRequest request) {
        Role role = findOrThrow(roleId);
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", request.getUserId()));

        // Dùng userRoles thay vì getRoles()
        boolean alreadyAssigned = user.getUserRoles().stream()
                .anyMatch(ur -> ur.getRole().getId().equals(roleId));
        if (alreadyAssigned) {
            throw new BusinessException(
                    "User '" + user.getEmail() + "' đã có role '" + role.getName() + "'"
            );
        }

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();
        user.getUserRoles().add(userRole);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void revokeUserFromRole(Long roleId, Long userId) {
        Role role = findOrThrow(roleId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));

        // Dùng userRoles thay vì getRoles()
        boolean removed = user.getUserRoles()
                .removeIf(ur -> ur.getRole().getId().equals(roleId));
        if (!removed) {
            throw new BusinessException(
                    "User '" + user.getEmail() + "' không có role '" + role.getName() + "' để thu hồi"
            );
        }

        userRepository.save(user);
    }

    // ─── Export ───────────────────────────────────────────────────────────────

    @Override
    public void exportRoles() {
        // TODO: inject ExportService hoặc dùng thư viện (Apache POI / OpenCSV)
        // Gợi ý flow:
        //   1. roleRepository.findAllWithPermissions()
        //   2. Map sang ExportRoleRow (id, name, slug, permissions, userCount)
        //   3. Ghi ra CSV/Excel rồi stream về HttpServletResponse
        throw new UnsupportedOperationException("exportRoles chưa được implement");
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private Role findOrThrow(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Role", id));
    }

    private RoleResponse toRoleResponse(Role r, Set<PermissionSummary> permissions, long userCount) {
        return new RoleResponse(
                r.getId(), r.getName(), r.getSlug(), r.getDescription(),
                r.getIsSystem(), r.getCreatedAt(), permissions, userCount
        );
    }

    private PermissionSummary toPermissionSummary(Permission p) {
        return new PermissionSummary(
                p.getId(), p.getName(), p.getSlug(), p.getModule(), p.getAction()
        );
    }

    private UserSummaryResponse toUserSummaryResponse(User u) {
        List<RoleSummary> roles = u.getUserRoles() == null
                ? List.of()
                : u.getUserRoles().stream()
                .map(ur -> new RoleSummary(
                        ur.getRole().getId(),
                        ur.getRole().getName(),
                        ur.getRole().getSlug()
                ))
                .toList();

        return new UserSummaryResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),        // thêm phone
                u.getAvatarUrl(),    // thêm avatarUrl
                u.getStatus(),
                u.getIsVerified(),   // thêm isVerified
                u.getCreatedAt(),
                roles                // thêm roles
        );
    }

    private RoleResponse toResponse(Role role) {
        return toResponse(role, 0L);
    }

    private RoleResponse toResponse(Role role, long userCount) {
        Set<PermissionSummary> permSummaries = role.getPermissions() == null
                ? Set.of()
                : role.getPermissions().stream()
                .map(this::toPermissionSummary)
                .collect(Collectors.toSet());

        return new RoleResponse(
                role.getId(), role.getName(), role.getSlug(), role.getDescription(),
                role.getIsSystem(), role.getCreatedAt(), permSummaries, userCount
        );
    }
}