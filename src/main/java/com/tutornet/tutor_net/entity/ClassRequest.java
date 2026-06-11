package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.enums.ClassRequestStatus;
import com.tutornet.tutor_net.enums.TeachingMode;
import jakarta.persistence.*;
import java.util.regex.Pattern;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

@Entity
@Table(name = "class_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_code", unique = true, nullable = false, length = 50)
    private String classCode;

    // Hàm chuyển không dấu
    private String removeVietnameseAccents(String str) {
        if (str == null) return null;
        String temp = str.replace("Đ", "D").replace("đ", "d");
        temp = Normalizer.normalize(temp, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(temp).replaceAll("").toUpperCase();
    }

    @PrePersist
    protected void onCreate() {
        if (this.classCode == null) {
            String subjectPrefix = "XX"; // Mặc định nếu lỗi

            // 1. Lấy 1 hoặc 2 chữ cái đầu của môn học
            if (this.subject != null && this.subject.getName() != null) {
                String cleanName = removeVietnameseAccents(this.subject.getName()).replaceAll("\\s+", "");
                if (cleanName.length() >= 2) {
                    subjectPrefix = cleanName.substring(0, 2);
                } else if (cleanName.length() == 1) {
                    subjectPrefix = cleanName; // Môn học chỉ có 1 chữ cái
                }
            }

            // 2. Format Thời gian: YYMMDDHHmm (10 ký tự)
            String timeString = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

            // 3. 3 số ngẫu nhiên (từ 000 đến 999)
            int randomNum = new Random().nextInt(1000);
            String randomString = String.format("%03d", randomNum); // Pad số 0 ở đầu nếu cần

            // 4. Ghép chuỗi (Tối đa 15 ký tự)
            this.classCode = subjectPrefix + timeString + randomString;
        }
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Có thể null nếu là khách vãng lai (Guest)

    @Column(name = "contact_name", nullable = false, length = 100)
    private String contactName;

    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    @Column(name = "contact_email")
    private String contactEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "grade_level", nullable = false, length = 50)
    private String gradeLevel;

    @Column(name = "proposed_price")
    private BigDecimal proposedPrice;

    @Column(name = "sessions_per_week")
    private Integer sessionsPerWeek;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "hourly_rate", precision = 15, scale = 2)
    private BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "teaching_mode", nullable = false, columnDefinition = "teaching_mode")
    private TeachingMode teachingMode = TeachingMode.ONLINE;

    @Column(name = "address_detail", columnDefinition = "TEXT")
    private String addressDetail;

    @Column(name = "student_notes", columnDefinition = "TEXT")
    private String studentNotes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_tutor_id")
    private TutorProfile targetTutor; // Có thể null nếu đăng công khai lên bảng tin

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "class_request_status")
    private ClassRequestStatus status = ClassRequestStatus.PENDING;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}