package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    // Kiểm tra user đã có role chưa (tránh gán trùng)
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    // Tìm bản ghi gán role cụ thể của user
    Optional<UserRole> findByUserIdAndRoleId(Long userId, Long roleId);

    // Tất cả role assignments của 1 user (kèm role data)
    @Query("""
            SELECT ur FROM UserRole ur
            JOIN FETCH ur.role r
            LEFT JOIN FETCH ur.assignedBy ab
            WHERE ur.user.id = :userId
            ORDER BY ur.createdAt DESC
            """)
    List<UserRole> findByUserIdWithDetails(@Param("userId") Long userId);

    // Xoá role assignment cụ thể
    @Modifying
    @Query("DELETE FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.id = :roleId")
    int deleteByUserIdAndRoleId(@Param("userId") Long userId, @Param("roleId") Long roleId);
}
