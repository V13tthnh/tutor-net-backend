package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    // Spring Data JPA sẽ tự động tạo câu query:
    // SELECT * FROM verification_tokens WHERE token = ? AND token_type = ?
    Optional<VerificationToken> findByTokenAndTokenType(String token, String tokenType);

}
