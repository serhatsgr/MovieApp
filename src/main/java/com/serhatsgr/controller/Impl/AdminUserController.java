package com.serhatsgr.controller.Impl;

import com.serhatsgr.dto.ApiSuccess;
import com.serhatsgr.dto.UserDto;
import com.serhatsgr.service.Impl.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')") // Sadece Admin Erişebilir
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiSuccess<List<UserDto>>> getAllUsers() {
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcılar listelendi", userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccess<UserDto>> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcı detayı", userService.getUserById(id)));
    }

    @PutMapping("/{id}/ban")
    public ResponseEntity<ApiSuccess<UserDto>> banUser(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcı durumu güncellendi", userService.toggleUserBan(id)));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiSuccess<UserDto>> changeRole(@PathVariable Long id) {
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcı rolü değiştirildi", userService.toggleUserRole(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccess<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiSuccess.of("Kullanıcı silindi", "Deleted ID: " + id));
    }
}