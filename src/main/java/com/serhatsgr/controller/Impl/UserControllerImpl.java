package com.serhatsgr.controller.Impl;

import com.serhatsgr.controller.IUserController;
import com.serhatsgr.dto.*;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.service.Impl.GoogleAuthService;
import com.serhatsgr.service.Impl.JwtService;
import com.serhatsgr.service.Impl.PasswordResetService;
import com.serhatsgr.service.Impl.UserService;
import com.serhatsgr.entity.User;
import com.serhatsgr.entity.Role;
import com.serhatsgr.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Slf4j
public class UserControllerImpl implements IUserController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordResetService passwordResetService;
    private final GoogleAuthService googleAuthService; // EKLENDİ
    private final UserRepository userRepository; // Role bilgisi için

    public UserControllerImpl(UserService userService, JwtService jwtService,
                              AuthenticationManager authenticationManager,
                              PasswordResetService passwordResetService,
                              GoogleAuthService googleAuthService,
                              UserRepository userRepository) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
        this.googleAuthService = googleAuthService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<ApiSuccess<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            if (!authentication.isAuthenticated()) {
                throw new BaseException(new ErrorMessage(MessageType.AUTHENTICATION_FAILED, "Geçersiz kullanıcı adı veya şifre"));
            }

            TokenPairDto tokenPair = jwtService.generateTokenPair(request.username());

            // Rolü bul
            User user = userRepository.findByUsername(request.username()).orElseThrow();
            String role = user.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("ROLE_USER");

            AuthResponse authResponse = AuthResponse.success(
                    tokenPair.accessToken(),
                    tokenPair.refreshToken(),
                    user.getUsername(),
                    role,
                    "Giriş başarılı"
            );

            return ResponseEntity.ok(ApiSuccess.of("Giriş başarılı", authResponse));

        } catch (BadCredentialsException | UsernameNotFoundException e) {
            throw new BaseException(new ErrorMessage(MessageType.AUTHENTICATION_FAILED, "Geçersiz kullanıcı adı veya şifre"));
        }
    }

    @PostMapping("/google")
    public ResponseEntity<ApiSuccess<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse response = googleAuthService.authenticateWithGoogle(request.idToken());
        return ResponseEntity.ok(ApiSuccess.of("Google girişi başarılı", response));
    }


    @PostMapping("/refresh-token")
    @Override
    public ResponseEntity<ApiSuccess<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String username = jwtService.refreshAccessToken(request.token());
        TokenPairDto newTokenPair = jwtService.generateTokenPair(username);

        User user = userRepository.findByUsername(username).orElseThrow();
        String role = user.getAuthorities().stream().findFirst().map(GrantedAuthority::getAuthority).orElse("ROLE_USER");

        AuthResponse authResponse = AuthResponse.success(
                newTokenPair.accessToken(),
                newTokenPair.refreshToken(),
                username,
                role,
                "Token başarıyla yenilendi"
        );
        return ResponseEntity.ok(ApiSuccess.of("Token başarıyla yenilendi", authResponse));
    }


    @PostMapping("/register")
    @Override
    public ResponseEntity<ApiSuccess<CreateUserResponse>> register(@Valid @RequestBody CreateUserRequest request) {
        CreateUserResponse response = userService.createUser(request);
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcı başarıyla oluşturuldu", response));
    }

    // ... forgotPassword, verifyOtp, resetPassword aynı kalır.
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiSuccess<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.initiatePasswordReset(request);
        return ResponseEntity.ok(ApiSuccess.of("Doğrulama kodu e-posta adresinize gönderildi.", "Email Sent"));
    }

    @PostMapping("/verify-reset-code")
    public ResponseEntity<ApiSuccess<String>> verifyResetCode(@Valid @RequestBody VerifyOtpRequest request) {
        String resetToken = passwordResetService.verifyOtp(request);
        return ResponseEntity.ok(ApiSuccess.of("Kod doğrulandı.", resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiSuccess<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request);
        return ResponseEntity.ok(ApiSuccess.of("Şifreniz başarıyla güncellendi.", "Success"));
    }
}