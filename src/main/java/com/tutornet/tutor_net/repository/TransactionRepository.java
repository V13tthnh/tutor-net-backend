package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.dto.response.dashboard.RecentTransactionData;
import com.tutornet.tutor_net.entity.Transaction;
import com.tutornet.tutor_net.enums.PaymentMethod;
import com.tutornet.tutor_net.enums.TransactionStatus;
import com.tutornet.tutor_net.repository.projection.RevenueContractProjection;
import com.tutornet.tutor_net.repository.projection.TimeSeriesProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant ;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * Tìm kiếm + lọc giao dịch theo nhiều tiêu chí.
     * Tất cả điều kiện đều optional (null = bỏ qua điều kiện đó).
     */
    @Query(value = """
    SELECT t.* FROM transactions t
    JOIN contracts c ON c.id = t.contract_id
    JOIN users u     ON u.id = t.user_id AND u.deleted_at IS NULL
    WHERE
        (CAST(:status AS TEXT) IS NULL OR t.status = :status)
        AND (CAST(:paymentMethod AS TEXT) IS NULL OR t.payment_method = :paymentMethod)
        AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR t.created_at >= CAST(:from AS TIMESTAMPTZ))
        AND (CAST(:to   AS TIMESTAMPTZ) IS NULL OR t.created_at <= CAST(:to   AS TIMESTAMPTZ))
        AND (
            CAST(:search AS TEXT) IS NULL OR CAST(:search AS TEXT) = ''
            OR LOWER(t.transaction_code) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(c.contract_number)  LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.full_name)        LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(u.email)            LIKE LOWER(CONCAT('%', :search, '%'))
        )
    ORDER BY t.created_at DESC
    """, nativeQuery = true)
    Page<Transaction> findAllForAdmin(
            @Param("status")        String status,
            @Param("paymentMethod") String paymentMethod,
            @Param("from")          Instant from,
            @Param("to")            Instant to,
            @Param("search")        String search,
            Pageable pageable
    );

    // ── KPI summary ───────────────────────────────────────────────────────

    @Query(value = """
    SELECT COUNT(*) FROM transactions t
    WHERE (CAST(:from AS TIMESTAMPTZ) IS NULL OR t.created_at >= CAST(:from AS TIMESTAMPTZ))
      AND (CAST(:to   AS TIMESTAMPTZ) IS NULL OR t.created_at <= CAST(:to   AS TIMESTAMPTZ))
    """, nativeQuery = true)
    long countAllInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query(value = """
    SELECT COALESCE(SUM(t.amount), 0) FROM transactions t
    WHERE t.status = 'SUCCESS'
      AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR t.paid_at >= CAST(:from AS TIMESTAMPTZ))
      AND (CAST(:to   AS TIMESTAMPTZ) IS NULL OR t.paid_at <= CAST(:to   AS TIMESTAMPTZ))
    """, nativeQuery = true)
    BigDecimal sumRevenueInRange(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
        SELECT t FROM Transaction t
        JOIN FETCH t.contract c
        JOIN FETCH t.user u
        WHERE t.id = :id
        """)
    Optional<Transaction> findByIdWithDetails(@Param("id") Long id);

    @Query(value = """
    SELECT COUNT(*) FROM transactions t
    WHERE t.status = :status
      AND (CAST(:from AS TIMESTAMPTZ) IS NULL OR t.created_at >= CAST(:from AS TIMESTAMPTZ))
      AND (CAST(:to   AS TIMESTAMPTZ) IS NULL OR t.created_at <= CAST(:to   AS TIMESTAMPTZ))
    """, nativeQuery = true)
    long countByStatusInRange(
            @Param("status") String status,
            @Param("from")   Instant from,
            @Param("to")     Instant to
    );

    // Dành cho Webhook tra cứu lại giao dịch
    Optional<Transaction> findByTransactionCode(String transactionCode);

    // Dành cho việc Check xem có giao dịch nào đang PENDING để tránh tạo rác DB
    @Query("SELECT t FROM Transaction t WHERE t.contract.id = :contractId AND t.status = :status")
    Optional<Transaction> findTopByContractIdAndStatusOrderByCreatedAtDesc(
            @Param("contractId") Long contractId,
            @Param("status") TransactionStatus status
    );

    // thống kê dashboard
    @Query("SELECT SUM(t.amount) FROM Transaction t WHERE t.status = 'SUCCESS' AND t.paidAt BETWEEN :fromDate AND :toDate")
    BigDecimal sumRevenueBetweenDates(@Param("fromDate") Instant fromDate, @Param("toDate") Instant toDate);

    @Query(value = """
    SELECT
        TO_CHAR(period, 'YYYY-MM-DD') AS timePeriod,
        SUM(t.amount)                 AS revenue,
        COUNT(DISTINCT c.id)          AS contractCount
    FROM transactions t
    LEFT JOIN contracts c ON t.contract_id = c.id
    CROSS JOIN LATERAL (
        SELECT DATE_TRUNC(CAST(:interval AS text), t.paid_at AT TIME ZONE 'Asia/Ho_Chi_Minh') AS period
    ) p
    WHERE t.status = 'SUCCESS'
      AND t.paid_at BETWEEN :from AND :to
    GROUP BY period
    ORDER BY period ASC
    """, nativeQuery = true)
    List<RevenueContractProjection> getRevenueAndContractChart(
            @Param("from")     Instant from,
            @Param("to")       Instant to,
            @Param("interval") String interval);

    @Query(value = """
    SELECT t.id               AS id,
           t.transaction_code AS transactionCode,
           u.full_name        AS tutorName,
           c.contract_number  AS contractNumber,
           t.amount           AS amount,
           t.payment_method   AS paymentMethod,
           t.status           AS status,
           t.paid_at          AS paidAt
    FROM transactions t
    JOIN contracts c ON t.contract_id = c.id
    JOIN users u     ON t.user_id     = u.id
    WHERE t.created_at BETWEEN :from AND :to
      AND t.status = 'SUCCESS'
    ORDER BY t.paid_at DESC
    """, nativeQuery = true)
    List<RecentTransactionData> findTop5RecentSuccessful(
            Instant  from, Instant  to, Pageable pageable);
}
