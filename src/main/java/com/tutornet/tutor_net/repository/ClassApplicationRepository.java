package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.ClassApplication;
import com.tutornet.tutor_net.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ClassApplicationRepository extends JpaRepository<ClassApplication, Long> {
    @Query("SELECT ca FROM ClassApplication ca " +
            "JOIN FETCH ca.tutor t " +
            "JOIN FETCH t.user u " +
            "WHERE ca.classRequest.id = :classRequestId " +
            "ORDER BY ca.createdAt DESC")
    List<ClassApplication> findByClassRequestId(@Param("classRequestId") Long classRequestId);

    boolean existsByClassRequestIdAndTutorId(Long requestId, Long tutorId);
    int countByClassRequestId(Long requestId);
    List<ClassApplication> findByClassRequestIdOrderByCreatedAtDesc(Long requestId);
    List<ClassApplication> findByClassRequestIdAndStatus(Long requestId, ApplicationStatus status);
}
