package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Long> {
    // Kiểm tra xem lớp học này đã được lập hợp đồng hay chưa để tránh tạo trùng lặp
    boolean existsByClassRequestId(Long requestId);
}
