package com.tutornet.tutor_net.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tutor_certificates")
@RequiredArgsConstructor
@Getter
@Setter
public class TutorCertificate {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @Column(nullable = false)
    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    @Column(name = "is_verified")
    private Boolean isVerified = false;
}
