package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findBySlug(String slug);

    boolean existsBySlug(String slug);

    boolean existsByName(String name);

    // Lấy tất cả permissions của 1 user qua user → roles → permissions
    @Query("""
            SELECT DISTINCT p FROM Permission p
            JOIN p.roles r
            JOIN r.userRoles ur
            WHERE ur.user.id = :userId
              AND (ur.expiresAt IS NULL OR ur.expiresAt > CURRENT_TIMESTAMP)
            """)
    Set<Permission> findAllByUserId(@Param("userId") Long userId);

    // Group by module cho trang quản lý phân quyền
    @Query("SELECT DISTINCT p.module FROM Permission p ORDER BY p.module")
    List<String> findDistinctModules();

    List<Permission> findByModule(String module);
}
