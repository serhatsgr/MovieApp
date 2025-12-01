package com.serhatsgr.service.Impl;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.serhatsgr.dto.AuthResponse;
import com.serhatsgr.dto.TokenPairDto;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    @Value("${google.client.id}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse authenticateWithGoogle(String idTokenString) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);

            if (idToken == null) {
                throw new BaseException(new ErrorMessage(MessageType.AUTHENTICATION_FAILED, "Geçersiz Google Token"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            log.info("Google girişi algılandı: {}", email);

            // Kullanıcıyı bul veya oluştur
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> createNewGoogleUser(email, name, pictureUrl));

            // Token üret
            TokenPairDto tokenPair = jwtService.generateTokenPair(user.getUsername());

            // Kullanıcının rolünü al
            String role = user.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)
                    .orElse("ROLE_USER");

            return AuthResponse.success(
                    tokenPair.accessToken(),
                    tokenPair.refreshToken(),
                    user.getUsername(),
                    role,
                    "Google ile giriş başarılı"
            );

        } catch (GeneralSecurityException | IOException e) {
            log.error("Google doğrulama hatası", e);
            throw new BaseException(new ErrorMessage(MessageType.AUTHENTICATION_FAILED, "Google kimlik doğrulaması başarısız oldu."));
        }
    }

    private User createNewGoogleUser(String email, String name, String pictureUrl) {
        log.info("Yeni Google kullanıcısı oluşturuluyor: {}", email);

        // Benzersiz username üretme
        String baseUsername = name.replaceAll("\\s+", "").toLowerCase();
        String username = baseUsername;
        int counter = 1;
        while (userRepository.findByUsername(username).isPresent()) {
            username = baseUsername + counter++;
        }

        User newUser = new User();
        newUser.setEmail(email);
        newUser.setUsername(username);
        // Google kullanıcılarının şifresi olmaz burada rastgele UUID atıyorum
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setAuthorities(Set.of(Role.ROLE_USER));
        newUser.setAccountNonExpired(true);
        newUser.setAccountNonLocked(true);
        newUser.setCredentialsNonExpired(true);
        newUser.setEnabled(true);
        newUser.setProvider("GOOGLE");
        newUser.setProfileImageUrl(pictureUrl);

        return userRepository.save(newUser);
    }
}