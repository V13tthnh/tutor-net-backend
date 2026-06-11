package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.enums.StudentInitiativeLvl;
import com.tutornet.tutor_net.enums.StudentProgressLvl;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "study_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @Column(name = "report_month", nullable = false)
    private Integer reportMonth;

    @Column(name = "report_year", nullable = false)
    private Integer reportYear;

    // Hibernate 6 sẽ tự động parse List này thành mảng JSONB lưu xuống database
//    @JdbcTypeCode(SqlTypes.JSON)
//    @Column(name = "session_details", columnDefinition = "jsonb", nullable = false)
//    private List<SessionDetail> sessionDetails = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "student_progress", columnDefinition = "student_progress_lvl")
    private StudentProgressLvl studentProgress = StudentProgressLvl.AVERAGE;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "student_initiative", columnDefinition = "student_initiative_lvl")
    private StudentInitiativeLvl studentInitiative = StudentInitiativeLvl.NEEDS_REMINDING;

    @Column(name = "improvement_points", columnDefinition = "TEXT")
    private String improvementPoints;

    @Column(name = "weak_points", columnDefinition = "TEXT")
    private String weakPoints;

    @Column(name = "next_month_plan", columnDefinition = "TEXT")
    private String nextMonthPlan;

    @Column(name = "suggestion_to_parent", columnDefinition = "TEXT")
    private String suggestionToParent;

    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
