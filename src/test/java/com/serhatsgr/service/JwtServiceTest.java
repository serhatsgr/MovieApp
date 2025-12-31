package com.serhatsgr.service;

import com.serhatsgr.entity.RefreshToken;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.repository.RefreshTokenRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.Key;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private JwtService jwtService;

    // Test için sabit bir secret key (Base64)
    private final String SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setup() {
        // @Value ile enjekte edilen değerleri manuel set ediyoruz
        ReflectionTestUtils.setField(jwtService, "SECRET", SECRET);
        ReflectionTestUtils.setField(jwtService, "ACCESS_TOKEN_EXPIRY", 1000L); // 1000 saniye
        ReflectionTestUtils.setField(jwtService, "REFRESH_TOKEN_EXPIRY", 60L);
    }

    @Test
    @DisplayName("generateAccessToken -> Token üretilmeli")
    void generateAccessToken_Success() {
        // Given
        User user = new User();
        user.setUsername("testUser");
        user.setAuthorities(Set.of(Role.ROLE_USER));

        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(user));

        // When
        String token = jwtService.generateAccessToken("testUser");

        // Then
        assertThat(token).isNotNull();
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("testUser");
    }

    @Test
    @DisplayName("validateToken -> Doğru kullanıcı ise true dönmeli")
    void validateToken_Success() {
        // Given
        User user = new User(); user.setUsername("testUser");
        given(userRepository.findByUsername("testUser")).willReturn(Optional.of(user)); // generate içinde kullanılıyor

        String token = jwtService.generateAccessToken("testUser");

        // When
        Boolean isValid = jwtService.validateToken(token, user);

        // Then
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("generateAccessToken -> Kullanıcı bulunamazsa NOT_FOUND")
    void generateAccessToken_UserNotFound() {
        given(userRepository.findByUsername("x")).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() -> jwtService.generateAccessToken("x"));

        assertThat(ex).isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("validateToken -> Username uyuşmazsa false dönmeli")
    void validateToken_WrongUser() {
        User real = new User(); real.setUsername("real");
        User fake = new User(); fake.setUsername("fake");

        given(userRepository.findByUsername("real")).willReturn(Optional.of(real));

        String token = jwtService.generateAccessToken("real");

        boolean result = jwtService.validateToken(token, fake);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateToken -> Expired token kontrolü hata vermeli")
    void validateToken_ExpiredToken() {
        // Manual expired token üretelim
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

        String expiredToken = Jwts.builder()
                .setSubject("test")
                .setExpiration(new Date(System.currentTimeMillis() - 10000))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        User user = new User(); user.setUsername("test");

        Throwable ex = catchThrowable(() -> jwtService.validateToken(expiredToken, user));

        assertThat(ex).isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("generateRefreshToken -> Token kaydedilmeli")
    void generateRefreshToken_Success() {
        given(refreshTokenRepository.save(any())).willAnswer(i -> i.getArgument(0));

        var response = jwtService.generateRefreshToken("serhat");

        assertThat(response.refreshToken()).isNotNull();
    }

    @Test
    @DisplayName("refreshAccessToken -> Refresh token bulunamazsa NOT_FOUND")
    void refreshAccessToken_NotFound() {
        given(refreshTokenRepository.findByToken("x")).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() -> jwtService.refreshAccessToken("x"));

        assertThat(ex).isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("refreshAccessToken -> Expired token UNAUTHORIZED")
    void refreshAccessToken_Expired() {
        RefreshToken expired = new RefreshToken("t", "user", LocalDateTime.now().minusMinutes(1));
        given(refreshTokenRepository.findByToken("t")).willReturn(Optional.of(expired));

        Throwable ex = catchThrowable(() -> jwtService.refreshAccessToken("t"));

        assertThat(ex).isInstanceOf(BaseException.class);
    }

    @Test
    @DisplayName("validateRefreshToken -> Süresi dolmuş false")
    void validateRefreshToken_Expired() {
        RefreshToken token = new RefreshToken("t", "user", LocalDateTime.now().minusMinutes(1));
        given(refreshTokenRepository.findByToken("t")).willReturn(Optional.of(token));

        boolean result = jwtService.validateRefreshToken("t");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateRefreshToken -> Used true ise false")
    void validateRefreshToken_Used() {
        RefreshToken token = new RefreshToken("t", "user", LocalDateTime.now().plusMinutes(5));
        token.setUsed(true);

        given(refreshTokenRepository.findByToken("t")).willReturn(Optional.of(token));

        boolean result = jwtService.validateRefreshToken("t");
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("validateRefreshToken -> Normal token true")
    void validateRefreshToken_Valid() {
        RefreshToken token = new RefreshToken("t", "user", LocalDateTime.now().plusMinutes(5));

        given(refreshTokenRepository.findByToken("t")).willReturn(Optional.of(token));

        boolean result = jwtService.validateRefreshToken("t");
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("revokeAllRefreshTokens -> Başarılı çalışmalı")
    void revokeAllRefreshTokens_Success() {
        jwtService.revokeAllRefreshTokens("serhat");
        verify(refreshTokenRepository).deleteByUsername("serhat");
    }

}