package com.tutornet.tutor_net.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "subjects",
        comment = "Danh mục môn học cấu trúc cây. Cấp 1: nhóm môn (Khoa học tự nhiên). Cấp 2: môn học (Toán). Cấp 3+: chuyên đề (Đại số, IELTS)."
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * NULL = danh mục gốc. Có giá trị = danh mục con.
     * Dùng CTE đệ quy để lấy toàn bộ cây.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Subject parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = false)
    @Builder.Default
    private List<Subject> children = new ArrayList<>();

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Dùng trong URL: /subjects/toan-hoc.
     * Không đổi sau khi đã xuất bản để tránh broken link.
     */
    @Column(name = "slug", nullable = false, unique = true, length = 200)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon_url", columnDefinition = "TEXT")
    private String iconUrl;

    /**
     * Soft disable thay vì DELETE để không ảnh hưởng đến dữ liệu
     * sessions và tutor_subjects cũ.
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMPTZ")
    @CreationTimestamp
    private OffsetDateTime createdAt;
}