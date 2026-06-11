package com.tutornet.tutor_net.entity;

import jakarta.persistence.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tutor_teaching_areas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TutorTeachingArea {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @Column(nullable = false, length = 100)
    private String province;

    @Column(length = 100)
    private String ward;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

