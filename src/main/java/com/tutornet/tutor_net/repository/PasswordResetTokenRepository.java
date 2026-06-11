package com.tutornet.tutor_net.repository;

import com.tutornet.tutor_net.entity.PasswordResetToken;
import com.tutornet.tutor_net.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    // Vô hiệu hoá tất cả token cũ của user khi đặt lại mật khẩu thành công
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.user = :user AND t.used = false")
    void invalidateAllTokensForUser(User user);
}
