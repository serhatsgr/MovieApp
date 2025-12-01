package com.serhatsgr.controller.Impl;

import com.serhatsgr.dto.ApiSuccess;
import com.serhatsgr.dto.AuthResponse;
import com.serhatsgr.dto.TokenPairDto;
import com.serhatsgr.dto.UpdateUserRequest;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.service.Impl.JwtService;
import com.serhatsgr.service.Impl.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/rest/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserService userService;
    private final JwtService jwtService;

    @PutMapping("/update")
    public ResponseEntity<ApiSuccess<AuthResponse>> updateUser(
            @Valid @RequestBody UpdateUserRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {

        User updatedUser = userService.updateUser(userDetails.getUsername(), request);


        TokenPairDto newTokenPair = jwtService.generateTokenPair(updatedUser.getUsername());


        String role = updatedUser.getAuthorities().stream()
                .findFirst()
                .map(Role::name)
                .orElse("ROLE_USER");


        AuthResponse response = AuthResponse.success(
                newTokenPair.accessToken(),
                newTokenPair.refreshToken(),
                updatedUser.getUsername(),
                role,
                "Profil başarıyla güncellendi"
        );

        return ResponseEntity.ok(ApiSuccess.of("Güncelleme başarılı", response));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<ApiSuccess<String>> deleteAccount(@AuthenticationPrincipal UserDetails userDetails) {
        userService.deleteUserByUsername(userDetails.getUsername());
        return ResponseEntity.ok(ApiSuccess.of("Hesabınız başarıyla silindi. Güle güle!", "Deleted"));
    }
}