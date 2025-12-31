package com.serhatsgr.service;

import com.serhatsgr.dto.ForgotPasswordRequest;
import com.serhatsgr.dto.ResetPasswordRequest;
import com.serhatsgr.dto.VerifyOtpRequest;
import com.serhatsgr.entity.PasswordResetToken;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.PasswordResetTokenRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.EmailService;
import com.serhatsgr.service.Impl.JwtService;
import com.serhatsgr.service.Impl.PasswordResetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks private PasswordResetService resetService;

    // ---------------------------------------------------------
    // initiatePasswordReset
    // ---------------------------------------------------------

    @Test
    @DisplayName("initiatePasswordReset -> Başarılı OTP üretimi ve mail gönderimi")
    void initiatePasswordReset_Success() {
        User user = new User(); user.setEmail("test@mail.com");
        given(userRepository.findByEmail("test@mail.com")).willReturn(Optional.of(user));
        given(tokenRepository.findByUser(user)).willReturn(Optional.empty());

        resetService.initiatePasswordReset(new ForgotPasswordRequest("test@mail.com"));

        verify(tokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendOtpEmail(any(), any());
    }

    @Test
    @DisplayName("initiatePasswordReset -> Kullanıcı yoksa NOT_FOUND fırlatmalı")
    void initiatePasswordReset_UserNotFound() {
        given(userRepository.findByEmail("test@mail.com")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(
                () -> resetService.initiatePasswordReset(new ForgotPasswordRequest("test@mail.com"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }

    @Test
    @DisplayName("initiatePasswordReset -> Google kullanıcıları için yasak")
    void initiatePasswordReset_GoogleUser() {
        User user = new User(); user.setEmail("test@mail.com"); user.setProvider("GOOGLE");
        given(userRepository.findByEmail("test@mail.com")).willReturn(Optional.of(user));

        Throwable thrown = catchThrowable(() ->
                resetService.initiatePasswordReset(new ForgotPasswordRequest("test@mail.com"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BUSINESS_RULE_VIOLATION);
    }

    @Test
    @DisplayName("initiatePasswordReset -> Süresi dolmamış aktif OTP varsa yeni kod gönderemez")
    void initiatePasswordReset_ActiveOtpExists() {
        User user = new User(); user.setEmail("test@mail.com");

        PasswordResetToken active = new PasswordResetToken("123456", user, 9999);
        active.setExpiryDate(LocalDateTime.now().plusMinutes(2));

        given(userRepository.findByEmail("test@mail.com")).willReturn(Optional.of(user));
        given(tokenRepository.findByUser(user)).willReturn(Optional.of(active));

        Throwable thrown = catchThrowable(() ->
                resetService.initiatePasswordReset(new ForgotPasswordRequest("test@mail.com"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BUSINESS_RULE_VIOLATION);
    }

    // ---------------------------------------------------------
    // verifyOtp
    // ---------------------------------------------------------

    @Test
    @DisplayName("verifyOtp -> Doğru OTP ile reset token üretmeli")
    void verifyOtp_Success() {
        User user = new User(); user.setEmail("test@mail.com");
        PasswordResetToken token = new PasswordResetToken("123456", user, 150);

        given(userRepository.findByEmail("test@mail.com")).willReturn(Optional.of(user));
        given(tokenRepository.findByOtpAndUser("123456", user)).willReturn(Optional.of(token));

        String resetToken = resetService.verifyOtp(new VerifyOtpRequest("test@mail.com", "123456"));

        assertThat(resetToken).isNotNull();
        verify(tokenRepository).save(token);
    }

    @Test
    @DisplayName("verifyOtp -> Kullanıcı bulunamaz")
    void verifyOtp_UserNotFound() {
        given(userRepository.findByEmail("mail@mail.com")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                resetService.verifyOtp(new VerifyOtpRequest("mail@mail.com", "123456"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.NOT_FOUND);
    }

    @Test
    @DisplayName("verifyOtp -> OTP eşleşmez → HATA")
    void verifyOtp_OtpNotFound() {
        User user = new User();
        given(userRepository.findByEmail("mail@mail.com")).willReturn(Optional.of(user));
        given(tokenRepository.findByOtpAndUser("000000", user)).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                resetService.verifyOtp(new VerifyOtpRequest("mail@mail.com", "000000"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BAD_REQUEST);
    }

    @Test
    @DisplayName("verifyOtp -> OTP süresi dolmuş → HATA")
    void verifyOtp_Expired() {
        User user = new User();
        PasswordResetToken token = new PasswordResetToken("123456", user, 150);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        given(userRepository.findByEmail("mail@mail.com")).willReturn(Optional.of(user));
        given(tokenRepository.findByOtpAndUser("123456", user)).willReturn(Optional.of(token));

        Throwable thrown = catchThrowable(() ->
                resetService.verifyOtp(new VerifyOtpRequest("mail@mail.com", "123456"))
        );

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BUSINESS_RULE_VIOLATION);

        verify(tokenRepository).delete(token);
    }

    // ---------------------------------------------------------
    // resetPassword
    // ---------------------------------------------------------

    @Test
    @DisplayName("resetPassword -> Başarılı şifre güncelleme")
    void resetPassword_Success() {
        User user = new User(); user.setUsername("testuser");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        given(tokenRepository.findByResetToken("validToken")).willReturn(Optional.of(token));
        given(passwordEncoder.encode("newPass")).willReturn("encodedPass");

        resetService.resetPassword(new ResetPasswordRequest("validToken", "newPass", "newPass"));

        verify(userRepository).save(user);
        verify(jwtService).revokeAllRefreshTokens("testuser");
        verify(tokenRepository).delete(token);
    }

    @Test
    @DisplayName("resetPassword -> Şifreler uyuşmaz → BAD_REQUEST")
    void resetPassword_Mismatch() {
        ResetPasswordRequest req = new ResetPasswordRequest("token", "123", "456");

        Throwable thrown = catchThrowable(() -> resetService.resetPassword(req));

        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BAD_REQUEST);
    }

    @Test
    @DisplayName("resetPassword -> Token bulunamaz")
    void resetPassword_TokenNotFound() {
        given(tokenRepository.findByResetToken("token")).willReturn(Optional.empty());

        Throwable thrown = catchThrowable(() ->
                resetService.resetPassword(new ResetPasswordRequest("token", "123", "123"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BAD_REQUEST);
    }

    @Test
    @DisplayName("resetPassword -> Token süresi dolmuş")
    void resetPassword_Expired() {
        User user = new User();
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().minusMinutes(1));

        given(tokenRepository.findByResetToken("token")).willReturn(Optional.of(token));

        Throwable thrown = catchThrowable(() ->
                resetService.resetPassword(new ResetPasswordRequest("token", "123", "123"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BUSINESS_RULE_VIOLATION);
    }

    @Test
    @DisplayName("resetPassword -> Google kullanıcılarının şifresi değiştirilemez")
    void resetPassword_GoogleUser() {
        User user = new User(); user.setProvider("GOOGLE");
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setExpiryDate(LocalDateTime.now().plusMinutes(5));

        given(tokenRepository.findByResetToken("token")).willReturn(Optional.of(token));

        Throwable thrown = catchThrowable(() ->
                resetService.resetPassword(new ResetPasswordRequest("token", "123", "123"))
        );

        assertThat(thrown).isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.BUSINESS_RULE_VIOLATION);
    }
}
