package com.serhatsgr.service.Impl;

import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendOtpEmail(String to, String otp) {
        try {
            log.info("📧 Mail gönderimi başlatıldı: {}", to);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("MovieApp <noreply@movieapp.com>"); // Görünen isim
            message.setTo(to);
            message.setSubject("Şifre Sıfırlama Kodu - MovieApp");
            message.setText("Merhaba,\n\n" +
                    "Şifre sıfırlama talebiniz için doğrulama kodunuz aşağıdadır:\n\n" +
                    "KOD: " + otp + "\n\n" +
                    "Bu kod 5 dakika boyunca geçerlidir.\n" +
                    "Eğer bu işlemi siz yapmadıysanız, bu maili dikkate almayınız.\n\n" +
                    "Sevgiler,\nMovieApp Ekibi");

            mailSender.send(message);

            log.info("✅ Mail başarıyla gönderildi: {}", to);

        } catch (Exception e) {
            log.error("❌ Mail gönderme hatası: {}", e.getMessage());
            // Mail hatası, kullanıcıya "Sunucu hatası" olarak dönmeli
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Mail gönderilemedi. Lütfen daha sonra tekrar deneyiniz."));
        }
    }
}