package com.serhatsgr.service;

import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.service.Impl.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Test
    @DisplayName("sendOtpEmail -> Başarılı gönderim")
    void sendOtpEmail_Success() {
        // Given
        String email = "test@mail.com";
        String otp = "123456";

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        // When
        emailService.sendOtpEmail(email, otp);

        // Then – mailSender çağrıldı mı?
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();

        // İçerik doğrulamaları
        assertThat(sentMessage.getTo()).contains(email);
        assertThat(sentMessage.getFrom()).isEqualTo("MovieApp <noreply@movieapp.com>");
        assertThat(sentMessage.getSubject()).isEqualTo("Şifre Sıfırlama Kodu - MovieApp");
        assertThat(sentMessage.getText()).contains(otp);
    }

    @Test
    @DisplayName("sendOtpEmail -> Hata durumunda BaseException fırlatmalı")
    void sendOtpEmail_Failure() {
        // Given
        doThrow(new RuntimeException("SMTP Error"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        // When
        Throwable thrown = catchThrowable(() ->
                emailService.sendOtpEmail("test@mail.com", "123456"));

        // Then
        assertThat(thrown)
                .isInstanceOf(BaseException.class)
                .extracting("errorMessage.messageType")
                .isEqualTo(MessageType.INTERNAL_ERROR);
    }
}
