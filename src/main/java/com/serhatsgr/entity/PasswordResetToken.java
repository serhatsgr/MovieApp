package com.serhatsgr.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String otp; // 6 haneli kod

    private String resetToken; // Doğrulama sonrası oluşan token

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    @OneToOne(targetEntity = User.class, fetch = FetchType.EAGER)
    @JoinColumn(nullable = false, name = "user_id")
    private User user;

    public PasswordResetToken(String otp, User user, long expirationInSeconds) {
        this.otp = otp;
        this.user = user;
        this.createdDate = LocalDateTime.now();
        // Dakika yerine saniye ekliyoruz
        this.expiryDate = LocalDateTime.now().plusSeconds(expirationInSeconds);
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryDate);
    }
}