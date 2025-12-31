package com.serhatsgr.service.Impl;

import com.serhatsgr.dto.*;
import com.serhatsgr.entity.PasswordResetToken;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.*;
import com.serhatsgr.repository.PasswordResetTokenRepository;
import com.serhatsgr.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public void initiatePasswordReset(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Bu e-posta ile kayıtlı kullanıcı bulunamadı.")));

        // Eğer kullanıcı Google ile kayıt olmuşsa şifre sıfırlama yapamaz.
        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            throw new BaseException(new ErrorMessage(
                    MessageType.BUSINESS_RULE_VIOLATION,
                    "Google hesabınızla giriş yapmalısınız. Şifre sıfırlama işlemi sadece e-posta ile kayıt olan kullanıcılar içindir."
            ));
        }
        // -----------------------------

        // Rate Limit Kontrolü
        tokenRepository.findByUser(user).ifPresent(t -> {
            if (!t.isExpired()) {
                throw new BaseException(new ErrorMessage(MessageType.BUSINESS_RULE_VIOLATION, "Mevcut kodunuzun süresi dolmadan yeni kod isteyemezsiniz."));
            }
            tokenRepository.delete(t);
        });

        // 6 Haneli OTP Üretme
        String otp = String.format("%06d", new Random().nextInt(999999));

        // 2.5 dakika geçerlilik süresi
        PasswordResetToken token = new PasswordResetToken(otp, user, 150);
        tokenRepository.save(token);

        emailService.sendOtpEmail(user.getEmail(), otp);
    }

    @Transactional
    public String verifyOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı.")));

        PasswordResetToken token = tokenRepository.findByOtpAndUser(request.otp(), user)
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Geçersiz kod.")));

        if (token.isExpired()) {
            tokenRepository.delete(token);
            throw new BaseException(new ErrorMessage(MessageType.BUSINESS_RULE_VIOLATION, "Kodun süresi dolmuş."));
        }

        // OTP Doğru -> Reset Token Üret
        String resetToken = UUID.randomUUID().toString();
        token.setResetToken(resetToken);
        tokenRepository.save(token);

        return resetToken;
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.newPassword().equals(request.confirmNewPassword())) {
            throw new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Şifreler uyuşmuyor."));
        }

        PasswordResetToken token = tokenRepository.findByResetToken(request.resetToken())
                .orElseThrow(() -> new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Geçersiz veya kullanılmış token.")));

        if (token.isExpired()) {
            throw new BaseException(new ErrorMessage(MessageType.BUSINESS_RULE_VIOLATION, "İşlem süresi dolmuş."));
        }

        User user = token.getUser();


        if ("GOOGLE".equalsIgnoreCase(user.getProvider())) {
            throw new BaseException(new ErrorMessage(MessageType.BUSINESS_RULE_VIOLATION, "Google hesabı şifresi değiştirilemez."));
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        // Kullanıcının tüm oturumlarını kapat
        jwtService.revokeAllRefreshTokens(user.getUsername());

        tokenRepository.delete(token);
    }
}