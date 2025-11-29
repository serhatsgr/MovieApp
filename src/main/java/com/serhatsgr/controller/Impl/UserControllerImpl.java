package com.serhatsgr.controller.Impl;

import com.serhatsgr.controller.IUserController;
import com.serhatsgr.dto.*;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.service.Impl.JwtService;
import com.serhatsgr.service.Impl.PasswordResetService;
import com.serhatsgr.service.Impl.UserService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    public UserControllerImpl(UserService userService, JwtService jwtService, AuthenticationManager authenticationManager, PasswordResetService passwordResetService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<ApiSuccess<AuthResponse>> login(@Valid @RequestBody AuthRequest request) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );

            if (!authentication.isAuthenticated()) {
                log.warn("Kullanıcı doğrulaması başarısız: {}", request.username());
                throw new BaseException(new ErrorMessage(
                        MessageType.AUTHENTICATION_FAILED,
                        "Geçersiz kullanıcı adı veya şifre"
                ));
            }

            TokenPairDto tokenPair = jwtService.generateTokenPair(request.username());
            log.info("Kullanıcı başarıyla giriş yaptı: {}", request.username());

            AuthResponse authResponse = AuthResponse.success(
                    tokenPair.accessToken(),
                    tokenPair.refreshToken(),
                    "Giriş başarılı"
            );

            return ResponseEntity.ok(ApiSuccess.of("Giriş başarılı", authResponse));

        } catch (BadCredentialsException | UsernameNotFoundException e) {
            log.warn("Authentication failed for user: {}", request.username());
            throw new BaseException(new ErrorMessage(
                    MessageType.AUTHENTICATION_FAILED,
                    "Geçersiz kullanıcı adı veya şifre"
            ));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during login for user: {}", request.username(), e);
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR,
                    "Giriş sırasında bir hata oluştu"
            ));
        }
    }

    @PostMapping("/refresh-token")
    @Override
    public ResponseEntity<ApiSuccess<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            if (refreshTokenRequest == null || refreshTokenRequest.token() == null || refreshTokenRequest.token().isBlank()) {
                throw new BaseException(new ErrorMessage(MessageType.BAD_REQUEST, "Refresh token gereklidir"));
            }

            String username = jwtService.refreshAccessToken(refreshTokenRequest.token());
            TokenPairDto newTokenPair = jwtService.generateTokenPair(username);

            AuthResponse authResponse = AuthResponse.success(
                    newTokenPair.accessToken(),
                    newTokenPair.refreshToken(),
                    "Token başarıyla yenilendi"
            );

            return ResponseEntity.ok(ApiSuccess.of("Token başarıyla yenilendi", authResponse));

        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token yenileme hatası: {}", e.getMessage(), e);
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Token yenilenirken hata oluştu: " + e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Override
    public ResponseEntity<ApiSuccess<CreateUserResponse>> register(@Valid @RequestBody CreateUserRequest request) {
        try {
            log.info("User registration attempt: {}", request.username());
            CreateUserResponse response = userService.createUser(request);
            log.info("User registered successfully: {}", request.username());
            return ResponseEntity.ok(ApiSuccess.of("Kullanıcı başarıyla oluşturuldu", response));
        } catch (BaseException e) {
            throw e;
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.warn("Data integrity violation during registration for user: {}", request.username());
            throw new BaseException(new ErrorMessage(
                    MessageType.DUPLICATE_RESOURCE,
                    "Bu kullanıcı adı veya email adresi zaten kullanımda"
            ));
        } catch (Exception e) {
            log.error("Unexpected error during registration for user: {}", request.username(), e);
            throw new BaseException(new ErrorMessage(
                    MessageType.INTERNAL_ERROR,
                    "Kullanıcı kaydedilirken hata oluştu"
            ));
        }
    }

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
        return ResponseEntity.ok(ApiSuccess.of("Şifreniz başarıyla güncellendi. Giriş yapabilirsiniz.", "Success"));
    }
}
