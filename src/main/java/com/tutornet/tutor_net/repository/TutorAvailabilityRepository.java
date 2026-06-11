package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.TutorAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TutorAvailabilityRepository extends JpaRepository<TutorAvailability, Long> {
    List<TutorAvailability> findByTutorId(Long tutorId);
    void deleteAllByTutorId(Long tutorId);
}
