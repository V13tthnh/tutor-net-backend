package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.ClassApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClassApplicationRepository extends JpaRepository<ClassApplication, Long> {
    boolean existsByClassRequestIdAndTutorId(Long requestId, Long tutorId);
    int countByClassRequestId(Long requestId);
}
