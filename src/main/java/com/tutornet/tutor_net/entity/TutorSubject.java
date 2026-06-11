package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.enums.ProficiencyLevel;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;

@Entity
@Table(name = "tutor_subjects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorSubject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "proficiency_level", columnDefinition = "proficiency_lvl")
    @Builder.Default
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.INTERMEDIATE;

    @Column(name = "hourly_rate", nullable = false)
    private BigDecimal hourlyRate;
}
