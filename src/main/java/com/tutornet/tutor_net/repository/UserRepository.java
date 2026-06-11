package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Role;
import com.tutornet.tutor_net.entity.User;
import com.tutornet.tutor_net.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>,
        JpaSpecificationExecutor<User> {

    // ANNOTATION EntityGraph: Báo cho JPA biết cần join sẵn các bảng này
    @EntityGraph(attributePaths = {"userRoles", "userRoles.role", "userRoles.role.permissions"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Tìm kiếm theo email / tên và lọc theo danh sách roles — dùng cho trang admin danh sách users
    @Query(
            value = """
        SELECT DISTINCT u FROM User u
        LEFT JOIN u.userRoles ur
        LEFT JOIN ur.role r
        WHERE (:keyword = ''
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:statuses IS NULL OR u.status IN :statuses)
          AND (:roleSlugs IS NULL OR r.slug IN :roleSlugs)
          AND r.id IN :adminRoleIds
        """,
            countQuery = """
        SELECT COUNT(DISTINCT u.id) FROM User u
        LEFT JOIN u.userRoles ur
        LEFT JOIN ur.role r
        WHERE (:keyword = ''
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:statuses IS NULL OR u.status IN :statuses)
          AND (:roleSlugs IS NULL OR r.slug IN :roleSlugs)
          AND r.id IN :adminRoleIds
        """
    )
    Page<User> searchUsers(
            @Param("keyword") String keyword,
            @Param("statuses") List<UserStatus> statuses,
            @Param("roleSlugs") List<String> roleSlugs,
            @Param("adminRoleIds") List<Long> adminRoleIds,
            Pageable pageable
    );

    // Tìm user kèm roles (tránh N+1 khi hiển thị danh sách)
    @Query("""
            SELECT DISTINCT u FROM User u
            LEFT JOIN FETCH u.userRoles ur
            LEFT JOIN FETCH ur.role
            WHERE u.id = :id
            """)
    Optional<User> findByIdWithRoles(@Param("id") Long id);

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.userRoles ur
        LEFT JOIN FETCH ur.role r
        LEFT JOIN FETCH r.permissions
        WHERE u.email = :email
    """)
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);

    @Query(
            value = """
        SELECT DISTINCT u FROM User u
        JOIN FETCH u.userRoles ur
        JOIN FETCH ur.role r
        WHERE r.id = :roleId
    """,
            countQuery = """
        SELECT COUNT(DISTINCT u.id) FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
        WHERE r.id = :roleId
    """
    )
    Page<User> findByRoleId(@Param("roleId") Long roleId, Pageable pageable);

    // Đếm users theo status (dùng cho dashboard admin)
    long countByStatus(UserStatus status);

    // Soft delete thủ công nếu cần gọi ngoài @SQLDelete
    @Modifying
    @Query("UPDATE User u SET u.deletedAt = CURRENT_TIMESTAMP WHERE u.id = :id AND u.deletedAt IS NULL")
    int softDeleteById(@Param("id") Long id);

    // Lấy các status đang tồn tại trong bảng users (chỉ admin roles)
    @Query("""
    SELECT DISTINCT u.status FROM User u
    JOIN u.userRoles ur
    JOIN ur.role r
    WHERE r.id IN :adminRoleIds
    """)
    List<UserStatus> findDistinctStatuses(@Param("adminRoleIds") List<Long> adminRoleIds);

    // Lấy các role đang tồn tại (chỉ admin roles)
    @Query("""
    SELECT DISTINCT r FROM Role r
    JOIN r.userRoles ur
    WHERE r.id IN :adminRoleIds
    """)
    List<Role> findAdminRoles(@Param("adminRoleIds") List<Long> adminRoleIds);

    @Query("""
        SELECT DISTINCT u FROM User u
        JOIN u.userRoles ur
        JOIN ur.role r
        WHERE r.slug IN ('admin', 'super_admin')
    """)
    List<User> findAllByRoleSlug(String slug);

    @Query("""
    SELECT DISTINCT u FROM User u
    JOIN u.userRoles ur
    JOIN ur.role r
    WHERE r.slug IN ('admin', 'super_admin')
    """)
    List<User> findAllAdmins();
}
