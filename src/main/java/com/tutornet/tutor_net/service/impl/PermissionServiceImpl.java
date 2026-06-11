package com.tutornet.tutor_net.service.impl;

import com.tutornet.tutor_net.dto.request.RolePermissionRequest.*;
import com.tutornet.tutor_net.dto.response.PermissionGroupResponse;
import com.tutornet.tutor_net.dto.response.UserRoleResponse.*;
import com.tutornet.tutor_net.entity.Permission;
import com.tutornet.tutor_net.exception.BusinessException;
import com.tutornet.tutor_net.exception.ResourceNotFoundException;
import com.tutornet.tutor_net.repository.PermissionRepository;
import com.tutornet.tutor_net.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionServiceImpl implements PermissionService {

    private final PermissionRepository permissionRepository;

    // lấy tất cả permissions nhóm theo module
    @Override
    public Map<String, List<PermissionSummary>> getAllGroupedByModule() {
        return permissionRepository.findAll().stream()
                .map(this::toSummary)
                .collect(Collectors.groupingBy(
                        PermissionSummary::module,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }

    @Override
    public List<PermissionSummary> getAll() {
        return permissionRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    @Override
    public List<PermissionGroupResponse> getGroupedPermissions() {
        return permissionRepository.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        Permission::getModule,          // group by module
                        Collectors.mapping(
                                this::toSummary,
                                Collectors.toList()
                        )
                ))
                .entrySet().stream()
                .map(e -> new PermissionGroupResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(PermissionGroupResponse::module))
                .toList();
    }

    @Override
    public PermissionSummary getById(Long id) {
        return toSummary(permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id)));
    }

    @Override
    @Transactional
    public PermissionSummary create(CreatePermissionRequest request) {
        if (permissionRepository.existsBySlug(request.slug())) {
            throw new BusinessException("Slug '" + request.slug() + "' đã tồn tại");
        }
        if (permissionRepository.existsByName(request.name())) {
            throw new BusinessException("Tên permission '" + request.name() + "' đã tồn tại");
        }
        Permission permission = Permission.builder()
                .name(request.name())
                .slug(request.slug())
                .module(request.module())
                .action(request.action())
                .description(request.description())
                .build();
        return toSummary(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public PermissionSummary update(Long id, UpdatePermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id));
        permission.setName(request.name());
        permission.setDescription(request.description());
        // Slug / module / action giữ nguyên — dùng trong code guard
        return toSummary(permissionRepository.save(permission));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Permission", id));
        // DB cascade ON DELETE CASCADE trong role_permissions sẽ tự dọn
        permissionRepository.delete(permission);
    }

    @Override
    public List<PermissionSummary> getUserPermissions(Long userId) {
        return permissionRepository.findAllByUserId(userId).stream()
                .map(this::toSummary)
                .toList();
    }

    // ── Helper ──
    private PermissionSummary toSummary(Permission p) {
        return new PermissionSummary(p.getId(), p.getName(), p.getSlug(), p.getModule(), p.getAction());
    }
}
