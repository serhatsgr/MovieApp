package com.serhatsgr.repository;

import com.serhatsgr.entity.RefreshToken;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = RefreshTokenRepositoryTest.TestConfig.class)
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;INIT=CREATE SCHEMA IF NOT EXISTS MOVIE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password="
})
class RefreshTokenRepositoryTest {

    @Configuration
    @EnableAutoConfiguration
    @EntityScan("com.serhatsgr.entity")
    @EnableJpaRepositories("com.serhatsgr.repository")
    static class TestConfig {}

    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private EntityManager entityManager;

    @Test
    @DisplayName("deleteExpiredTokens -> Tarihi geçmiş tokenları silmeli")
    void deleteExpiredTokens_Success() {
        // Given
        RefreshToken expired = new RefreshToken();
        expired.setToken("expired-token-123");
        expired.setUsername("user1");
        expired.setExpiryDate(LocalDateTime.now().minusHours(1));
        refreshTokenRepository.save(expired);

        RefreshToken valid = new RefreshToken();
        valid.setToken("valid-token-456");
        valid.setUsername("user2");
        valid.setExpiryDate(LocalDateTime.now().plusHours(1));
        refreshTokenRepository.save(valid);

        // When
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
        entityManager.clear(); // DB ile senkronize et

        // Then
        assertThat(refreshTokenRepository.findByToken("expired-token-123")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("valid-token-456")).isPresent();
    }

    @Test
    @DisplayName("markAllAsUsedByUsername -> Kullanıcının tüm tokenlarını 'used' yapmalı")
    void markAllAsUsedByUsername_Success() {
        // Given
        RefreshToken t1 = new RefreshToken();
        t1.setToken("t1"); t1.setUsername("targetUser"); t1.setUsed(false); t1.setExpiryDate(LocalDateTime.now().plusDays(1));
        refreshTokenRepository.save(t1);

        RefreshToken t2 = new RefreshToken();
        t2.setToken("t2"); t2.setUsername("otherUser"); t2.setUsed(false); t2.setExpiryDate(LocalDateTime.now().plusDays(1));
        refreshTokenRepository.save(t2);

        // When
        refreshTokenRepository.markAllAsUsedByUsername("targetUser");
        entityManager.clear();

        // Then
        RefreshToken updatedT1 = refreshTokenRepository.findByToken("t1").get();
        RefreshToken untouchedT2 = refreshTokenRepository.findByToken("t2").get();

        assertThat(updatedT1.isUsed()).isTrue();
        assertThat(untouchedT2.isUsed()).isFalse();
    }
}