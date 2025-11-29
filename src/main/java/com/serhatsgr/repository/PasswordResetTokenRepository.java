package com.serhatsgr.repository;

import com.serhatsgr.entity.PasswordResetToken;
import com.serhatsgr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByOtpAndUser(String otp, User user);
    Optional<PasswordResetToken> findByResetToken(String resetToken);
    Optional<PasswordResetToken> findByUser(User user);
    void deleteByUser(User user);
}