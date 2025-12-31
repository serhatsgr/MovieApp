package com.serhatsgr.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.serhatsgr.dto.AuthResponse;
import com.serhatsgr.dto.TokenPairDto;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.GoogleAuthService;
import com.serhatsgr.service.Impl.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private GoogleAuthService googleAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(googleAuthService, "googleClientId", "mock-client-id");
    }

    @Test
    @DisplayName("authenticateWithGoogle -> Başarılı giriş (Mevcut Kullanıcı)")
    void authenticateWithGoogle_Success() {
        String idTokenString = "valid-token-string";

        // 1. Google'dan dönecek sahte veriler
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("test@gmail.com");
        payload.set("name", "Test User");
        payload.set("picture", "http://pic.url");

        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        given(googleIdToken.getPayload()).willReturn(payload);

        // 2. Verifier'ın sahtesi
        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);

        // 3. Kullanıcı verileri
        User user = new User();
        user.setUsername("testuser");
        user.setAuthorities(Set.of(Role.ROLE_USER));

        // 4. Mock Davranışları
        given(userRepository.findByEmail("test@gmail.com")).willReturn(Optional.of(user));
        given(jwtService.generateTokenPair("testuser")).willReturn(new TokenPairDto("access", "refresh"));

        // 5. CONSTRUCTOR MOCKING
        // Kod içinde "new GoogleIdTokenVerifier.Builder(...)" dendiğinde araya giriyoruz.
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    given(mock.setAudience(anyList())).willReturn(mock);
                    given(mock.build()).willReturn(mockVerifier);
                }
        )) {
            // verify() metodunun mockVerifier üzerinden çağrılacağını belirtiyoruz
            // (Try bloğu içinde olmalı çünkü mockVerifier ancak build() sonrası erişilebilir oluyor mantıken,
            // ama biz yukarıda instance'ı hazırladığımız için directly stublayabiliriz)
            given(mockVerifier.verify(idTokenString)).willReturn(googleIdToken);

            // Act
            AuthResponse response = googleAuthService.authenticateWithGoogle(idTokenString);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.username()).isEqualTo("testuser");
            assertThat(response.accessToken()).isEqualTo("access");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("authenticateWithGoogle -> Token geçersizse hata fırlatmalı")
    void authenticateWithGoogle_InvalidToken() {
        String idTokenString = "invalid-token";

        // Sahte Verifier
        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);

        // Constructor Mocking
        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    given(mock.setAudience(anyList())).willReturn(mock);
                    given(mock.build()).willReturn(mockVerifier);
                }
        )) {
            // verify() null dönerse token geçersiz demektir
            given(mockVerifier.verify(idTokenString)).willReturn(null);

            // Act
            Throwable thrown = catchThrowable(() -> googleAuthService.authenticateWithGoogle(idTokenString));

            // Assert
            assertThat(thrown)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorMessage.messageType")
                    .isEqualTo(MessageType.AUTHENTICATION_FAILED);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("authenticateWithGoogle -> Yeni kullanıcı oluşturmalı")
    void authenticateWithGoogle_NewUser() {
        String idTokenString = "new-user-token";

        // Google Verileri
        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("new@gmail.com");
        payload.set("name", "New User");
        payload.set("picture", "pic.jpg");

        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        given(googleIdToken.getPayload()).willReturn(payload);

        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);

        // Repo Mockları
        given(userRepository.findByEmail("new@gmail.com")).willReturn(Optional.empty()); // Kullanıcı yok
        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty()); // Username müsait
        given(passwordEncoder.encode(anyString())).willReturn("encodedPass");

        User savedUser = new User();
        savedUser.setUsername("newuser");
        savedUser.setAuthorities(Set.of(Role.ROLE_USER));
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        given(jwtService.generateTokenPair("newuser")).willReturn(new TokenPairDto("access", "refresh"));

        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder = Mockito.mockConstruction(
                GoogleIdTokenVerifier.Builder.class,
                (mock, context) -> {
                    given(mock.setAudience(anyList())).willReturn(mock);
                    given(mock.build()).willReturn(mockVerifier);
                }
        )) {
            given(mockVerifier.verify(idTokenString)).willReturn(googleIdToken);

            AuthResponse response = googleAuthService.authenticateWithGoogle(idTokenString);

            assertThat(response.username()).isEqualTo("newuser");
            verify(userRepository).save(any(User.class)); // Kayıt yapıldı mı?
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    @DisplayName("authenticateWithGoogle -> Google API exception fırlatırsa BaseException dönmeli")
    void authenticateWithGoogle_ApiException() throws GeneralSecurityException, IOException {
        String token = "api-error-token";

        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);

        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class,
                             (mock, context) -> {
                                 given(mock.setAudience(anyList())).willReturn(mock);
                                 given(mock.build()).willReturn(mockVerifier);
                             }
                     )) {

            given(mockVerifier.verify(token)).willThrow(new GeneralSecurityException("Test"));

            Throwable thrown = catchThrowable(() -> googleAuthService.authenticateWithGoogle(token));

            assertThat(thrown)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorMessage.messageType")
                    .isEqualTo(MessageType.AUTHENTICATION_FAILED);
        }
    }

    @Test
    @DisplayName("authenticateWithGoogle -> Kullanıcı rolü yoksa ROLE_USER dönmeli")
    void authenticateWithGoogle_DefaultRole() throws GeneralSecurityException, IOException{
        String token = "role-test";

        GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
        payload.setEmail("role@test.com");
        payload.set("name", "Role Test");

        GoogleIdToken googleIdToken = mock(GoogleIdToken.class);
        given(googleIdToken.getPayload()).willReturn(payload);

        GoogleIdTokenVerifier mockVerifier = mock(GoogleIdTokenVerifier.class);

        User user = new User();
        user.setUsername("rolename");
        user.setAuthorities(Set.of()); // BOŞ rol

        given(userRepository.findByEmail("role@test.com")).willReturn(Optional.of(user));
        given(jwtService.generateTokenPair("rolename"))
                .willReturn(new TokenPairDto("access", "refresh"));

        try (MockedConstruction<GoogleIdTokenVerifier.Builder> mockedBuilder =
                     Mockito.mockConstruction(GoogleIdTokenVerifier.Builder.class,
                             (mock, context) -> {
                                 given(mock.setAudience(anyList())).willReturn(mock);
                                 given(mock.build()).willReturn(mockVerifier);
                             }
                     )) {

            given(mockVerifier.verify(token)).willReturn(googleIdToken);

            AuthResponse response = googleAuthService.authenticateWithGoogle(token);

            assertThat(response.role()).isEqualTo("ROLE_USER");
        }
    }

    @Test
    @DisplayName("createNewGoogleUser -> Username çakışırsa increment etmeli")
    void createNewGoogleUser_UsernameCollision() {
        String email = "dup@gmail.com";
        String name = "Test User";
        String picture = "pic.jpg";

        // İlk username dolu
        given(userRepository.findByUsername("testuser")).willReturn(Optional.of(new User()));
        // İkinci username boş
        given(userRepository.findByUsername("testuser1")).willReturn(Optional.empty());

        given(passwordEncoder.encode(anyString())).willReturn("encoded");

        User saved = new User();
        saved.setUsername("testuser1");
        saved.setAuthorities(Set.of(Role.ROLE_USER));

        given(userRepository.save(any(User.class))).willReturn(saved);

        User response = ReflectionTestUtils.invokeMethod(
                googleAuthService,
                "createNewGoogleUser",
                email, name, picture
        );

        assertThat(response.getUsername()).isEqualTo("testuser1");
    }


    //tüm alanlar doğru set edildi mi?
    @Test
    @DisplayName("createNewGoogleUser -> Alanlar doğru atanmalı")
    void createNewGoogleUser_FieldCheck() {

        given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());
        given(passwordEncoder.encode(anyString())).willReturn("enc");

        User saved = new User();
        saved.setUsername("newuser");
        saved.setProvider("GOOGLE");
        saved.setAuthorities(Set.of(Role.ROLE_USER));

        given(userRepository.save(any(User.class))).willReturn(saved);

        User created = ReflectionTestUtils.invokeMethod(
                googleAuthService,
                "createNewGoogleUser",
                "x@gmail.com",
                "Test User",
                "picxx"
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User u = captor.getValue();
        assertThat(u.getProvider()).isEqualTo("GOOGLE");
        assertThat(u.getProfileImageUrl()).isEqualTo("picxx");
        assertThat(u.isEnabled()).isTrue();
        assertThat(u.getAuthorities()).contains(Role.ROLE_USER);
    }

}