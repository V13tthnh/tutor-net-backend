package com.tutornet.tutor_net.entity;

import com.tutornet.tutor_net.enums.ContractStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contract_number", unique = true, length = 100)
    private String contractNumber;

    // 1. LIÊN KẾT ĐỐI TƯỢNG (Ai dạy lớp nào?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    private ClassRequest classRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tutor_id", nullable = false)
    private TutorProfile tutor;

    // 2. THÔNG TIN TÀI CHÍNH (Thu bao nhiêu, Bao giờ thu, Thu chưa?)
    @Column(name = "introduction_fee", nullable = false)
    private BigDecimal introductionFee;

    @Column(name = "fee_payment_deadline")
    private LocalDate feePaymentDeadline; // Hạn chót đóng phí (Ngày giao lớp + 35 ngày)

    @Column(name = "is_fee_paid", nullable = false)
    private Boolean isFeePaid = false; // Đã thanh toán phí môi giới chưa?

    @Column(name = "paid_at")
    private LocalDateTime paidAt; // Thời điểm thanh toán thành công (Webhook trả về)

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate; // Ngày bắt đầu tính phí/dạy thử

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "contract_status")
    private ContractStatus status = ContractStatus.DRAFT;

    @Column(name = "free_trial_count")
    private Integer freeTrialCount;

    @Column(name = "contract_file_url", columnDefinition = "TEXT")
    private String contractFileUrl;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}