package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.TutorCertificate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorCertificateRepository extends JpaRepository<TutorCertificate, Long> {
    List<TutorCertificate> findByTutorId(Long tutorId);
}
