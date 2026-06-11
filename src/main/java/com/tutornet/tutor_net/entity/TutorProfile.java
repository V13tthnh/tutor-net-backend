package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.converter.TeachingModeArrayConverter;
import com.tutornet.tutor_net.enums.EduLevel;
import com.tutornet.tutor_net.enums.TeachingMode;
import com.tutornet.tutor_net.enums.TutorStatus;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import lombok.*;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "tutor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private Integer experienceYears = 0;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "education_level", columnDefinition = "edu_level")
    private EduLevel educationLevel;

    @Column(name = "is_available", nullable = false)
    @Builder.Default
    private Boolean isAvailable = true;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "teaching_mode", columnDefinition = "teaching_mode", nullable = false)
    @Builder.Default
    private TeachingMode teachingMode = TeachingMode.ONLINE;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", columnDefinition = "tutor_status", nullable = false)
    @Builder.Default
    private TutorStatus status = TutorStatus.DRAFT;

    @Column(name = "rating_avg", nullable = false, precision = 3, scale = 2)
    @Builder.Default
    private BigDecimal ratingAvg = BigDecimal.ZERO;

    @Column(name = "rating_count", nullable = false)
    @Builder.Default
    private Integer ratingCount = 0;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    @Column(length = 50)
    private String occupation;

    @Column(name = "student_year")
    private Integer studentYear;

    @Column(length = 200)
    private String major;

    @Column(length = 255)
    private String university;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(columnDefinition = "TEXT")
    private String achievements;

    @Column(name = "id_card_front_url", columnDefinition = "TEXT")
    private String idCardFrontUrl;

    @Column(name = "terms_accepted_at")
    private Instant termsAcceptedAt;

    @Formula("(SELECT MIN(ts.hourly_rate) FROM tutor_subjects ts WHERE ts.tutor_id = id)")
    private BigDecimal minHourlyRate;

    // --- CÁC BẢNG QUAN HỆ (Vẫn giữ nguyên vì ánh xạ tới các bảng phụ) ---
    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TutorSubject> subjects = new HashSet<>();

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TutorCertificate> certificates = new HashSet<>();

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private Set<TutorAvailability> availability = new HashSet<>();

    @OneToMany(mappedBy = "tutor", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<TutorTeachingArea> teachingAreas = new HashSet<>();

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    // --- AUDIT FIELDS ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}