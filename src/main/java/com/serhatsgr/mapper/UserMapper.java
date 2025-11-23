package com.serhatsgr.mapper;

import com.serhatsgr.dto.CreateUserResponse;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class UserMapper {

    // Entity -> DTO dönüşümü
    public CreateUserResponse toDto(User user, String message) {
        if (user == null) return CreateUserResponse.error("User is null");

        String roles = user.getAuthorities() != null && !user.getAuthorities().isEmpty()
                ? user.getAuthorities().stream()
                .map(Role::name)
                .collect(Collectors.joining(", "))
                : "NO_ROLE";

        return CreateUserResponse.success(
                user.getUsername(),
                user.getEmail(),
                roles,
                message
        );
    }

    // Hata durumlarında DTO dönüşümü
    public CreateUserResponse toError(String message) {
        return CreateUserResponse.error(message);
    }

    // DTO -> Entity dönüşümü (yeni kullanıcı oluşturma)
    public User toEntity(String username, String email, String password, Role role) {
        if (username == null || email == null || password == null) return null;

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(password);
        user.setAuthorities(Set.of(role));
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);

        return user;
    }

    // Var olan kullanıcıyı güncelleme
    public void updateEntity(User user, String email, String password, Set<Role> roles) {
        if (email != null && !email.isBlank()) {
            user.setEmail(email);
        }
        if (password != null && !password.isBlank()) {
            user.setPassword(password);
        }
        if (roles != null && !roles.isEmpty()) {
            user.setAuthorities(roles);
        }
    }
}
