package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.enums.GenderType;
import com.tutornet.tutor_net.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 20)
    private String phone;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "gender_type")
    @Builder.Default
    private GenderType gender = GenderType.OTHER;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "user_status")
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "login_count", nullable = false)
    @Builder.Default
    private Integer loginCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "social_links", columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, String> socialLinks = new HashMap<>();

    @Column(name = "birth_year")
    private Integer birthYear;

    @Column(name = "hometown_address", columnDefinition = "TEXT")
    private String hometownAddress;

    @Column(name = "current_address", columnDefinition = "TEXT")
    private String currentAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // Quan hệ 1-N với UserRole
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private Set<UserRole> userRoles = new HashSet<>();

    public boolean hasRole(String roleSlug) {
        return this.userRoles.stream()
                .anyMatch(userRole -> roleSlug.equals(userRole.getRoleSlug()));
    }
}