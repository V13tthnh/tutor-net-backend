package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.dto.request.UserRequest;
import com.tutornet.tutor_net.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role,Long>, JpaSpecificationExecutor<Role> {
    Optional<Role> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    // Fetch kèm permissions để tránh N+1
    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissions
            WHERE r.id = :id
            """)
    Optional<Role> findByIdWithPermissions(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT r FROM Role r
        LEFT JOIN FETCH r.permissions
        WHERE r.id IN :ids
    """)
    List<Role> findWithPermissionsByIds(@Param("ids") List<Long> ids);

    // Đếm số user theo từng roleId (trả Map để lookup O(1))
    @Query("""
        SELECT ur.role.id AS roleId, COUNT(DISTINCT ur.user.id) AS cnt
        FROM UserRole ur
        WHERE ur.role.id IN :roleIds
        GROUP BY ur.role.id
    """)
    List<Object[]> countUsersByRoleIds(@Param("roleIds") List<Long> roleIds);

    // Danh sách tất cả roles kèm permissions (trang quản lý phân quyền)
    @Query("""
            SELECT DISTINCT r FROM Role r
            LEFT JOIN FETCH r.permissions
            ORDER BY r.isSystem DESC, r.name ASC
            """)
    List<Role> findAllWithPermissions();

    // Đếm user đang giữ role này (hiển thị trong bảng roles)
    @Query("""
        SELECT COUNT(DISTINCT ur.user.id)
        FROM UserRole ur
        WHERE ur.role.id = :roleId
    """)
    long countUsersByRoleId(@Param("roleId") Long roleId);
}
