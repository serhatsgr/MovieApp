package com.serhatsgr.repository;

import com.serhatsgr.entity.RefreshToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByToken(String token);

    Optional<RefreshToken> findByUsernameAndIsUsedFalse(String username);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiryDate < :now")
    void deleteExpiredTokens(LocalDateTime now);

    @Modifying
    @Transactional
    void deleteByUsername(String username);

    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.isUsed = true WHERE rt.username = :username")
    void markAllAsUsedByUsername(String username);
    //kullanıcının tüm tokenlerını kullanıldı olarak işaretler

}
