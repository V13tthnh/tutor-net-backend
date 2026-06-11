package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.enums.InvitationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tutor_invitations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TutorInvitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    // Null nếu khách vãng lai
    @Column(name = "student_user_id")
    private Long studentUserId;

    @Column(name = "student_name", nullable = false, length = 100)
    private String studentName;

    @Column(name = "student_phone", nullable = false, length = 20)
    private String studentPhone;

    @Column(name = "student_email", nullable = false, length = 255)
    private String studentEmail;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvitationStatus status = InvitationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
