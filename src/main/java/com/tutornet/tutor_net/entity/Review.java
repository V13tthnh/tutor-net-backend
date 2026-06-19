package com.tutornet.tutor_net.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant ;

@Entity
@Table(name = "reviews")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id", nullable = false, unique = true)
    private Contract contract; // Mỗi hợp đồng/lớp học thành công chỉ được đánh giá 1 lần duy nhất

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer; // Có thể null nếu Khách vãng lai đánh giá qua token bảo mật

    @Column(name = "guest_review_token", unique = true)
    private String guestReviewToken; // Sử dụng cho giải pháp Magic Link gửi qua email

    @Column(name = "rating", nullable = false)
    private Integer rating; // Giá trị từ 1 đến 5 sao

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_public", nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant  createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant  updatedAt;
}
