package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Contract;
import com.tutornet.tutor_net.enums.ContractStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    boolean existsByClassRequestId(Long requestId);

    @Query("SELECT c FROM Contract c " +
            "LEFT JOIN c.tutor t " +
            "LEFT JOIN t.user tu " +
            "LEFT JOIN c.classRequest cr " +
            "LEFT JOIN cr.subject s " +
            "WHERE (t.user.id = :userId OR cr.user.id = :userId) " +
            // 🌟 Dùng cờ boolean thay vì kiểm tra IS NULL trực tiếp
            "  AND (:hasStatus = false OR c.status = :status) " +
            "  AND (:hasKeyword = false OR " +
            "       LOWER(c.contractNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(cr.classCode) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(tu.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "       OR LOWER(cr.contactName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Contract> searchMyContracts(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("hasKeyword") boolean hasKeyword,
            @Param("status") ContractStatus status,
            @Param("hasStatus") boolean hasStatus,
            Pageable pageable
    );
}
