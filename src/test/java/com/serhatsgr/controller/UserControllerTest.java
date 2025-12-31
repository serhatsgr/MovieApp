package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.UserControllerImpl;
import com.serhatsgr.dto.*;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.service.Impl.GoogleAuthService;
import com.serhatsgr.service.Impl.JwtService;
import com.serhatsgr.service.Impl.PasswordResetService;
import com.serhatsgr.service.Impl.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserControllerImpl.class)
@ContextConfiguration(classes = UserControllerTest.TestConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(UserControllerImpl.class)
    static class TestConfig {}

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private AuthenticationManager authenticationManager;
    @MockitoBean private PasswordResetService passwordResetService;
    @MockitoBean private GoogleAuthService googleAuthService;


    // --------------------------
    // LOGIN TEST
    // --------------------------
    @Test
    @DisplayName("POST /auth/login -> Başarılı giriş")
    void login_success() throws Exception {
        AuthRequest req = new AuthRequest("user", "123");

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "user", "123", List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        User u = new User();
        u.setUsername("user");
        u.setAuthorities(Set.of(Role.ROLE_USER));

        given(authenticationManager.authenticate(any())).willReturn(auth);
        given(jwtService.generateTokenPair("user")).willReturn(new TokenPairDto("access", "refresh"));


        given(userService.getUserByUsername("user")).willReturn(u);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.accessToken").value("access"));
    }

    // --------------------------
    // GOOGLE LOGIN TEST
    // --------------------------
    @Test
    @DisplayName("POST /auth/google -> Google login başarılı")
    void googleLogin_success() throws Exception {
        GoogleLoginRequest req = new GoogleLoginRequest("token123");

        AuthResponse resp = AuthResponse.success(
                "access", "refresh", "googleUser", "ROLE_USER", "ok"
        );

        given(googleAuthService.authenticateWithGoogle("token123")).willReturn(resp);

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("googleUser"));
    }

    // --------------------------
    // REFRESH TOKEN TEST
    // --------------------------
    @Test
    @DisplayName("POST /auth/refresh-token -> Başarılı yenileme")
    void refreshToken_success() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest("oldtoken");
        User u = new User();
        u.setUsername("serhat");
        u.setAuthorities(Set.of(Role.ROLE_USER));

        given(jwtService.refreshAccessToken("oldtoken")).willReturn("serhat");
        given(jwtService.generateTokenPair("serhat")).willReturn(new TokenPairDto("newA", "newR"));

        // DÜZELTME: Artık repository değil, service çağrılıyor.
        given(userService.getUserByUsername("serhat")).willReturn(u);

        mockMvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("serhat"))
                .andExpect(jsonPath("$.data.accessToken").value("newA"));
    }

    // --------------------------
    // REGISTER TEST
    // --------------------------
    @Test
    @DisplayName("POST /auth/register -> Başarılı kayıt")
    void register_success() throws Exception {
        CreateUserRequest req = new CreateUserRequest("newUser", "mail@test.com", "ValidPass123!", Set.of(Role.ROLE_USER));

        CreateUserResponse resp = new CreateUserResponse("newUser", "mail@test.com", "ROLE_USER", "Success", true);

        given(userService.createUser(any(CreateUserRequest.class))).willReturn(resp);

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("newUser"))
                .andExpect(jsonPath("$.data.success").value(true));
    }


    @Test
    @DisplayName("POST /auth/forgot-password -> Email gönderildi")
    void forgotPassword_success() throws Exception {
        ForgotPasswordRequest req = new ForgotPasswordRequest("mail@test.com");
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/verify-reset-code -> Kod doğrulandı")
    void verifyResetCode_success() throws Exception {
        VerifyOtpRequest req = new VerifyOtpRequest("mail@test.com", "123456");
        given(passwordResetService.verifyOtp(any())).willReturn("reset-token");
        mockMvc.perform(post("/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/reset-password -> Şifre güncellendi")
    void resetPassword_success() throws Exception {
        ResetPasswordRequest req = new ResetPasswordRequest("token", "ValidNewPass123", "ValidNewPass123");
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Şifreniz başarıyla güncellendi."));
    }
}