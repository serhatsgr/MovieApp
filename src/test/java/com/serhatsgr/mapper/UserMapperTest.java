package com.serhatsgr.mapper;

import com.serhatsgr.dto.CreateUserResponse;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    // ----------------------------------------------------------
    // toDto (SUCCESS)
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDto -> User entity doğru şekilde DTO'ya dönüştürülmeli")
    void toDto_Success() {
        // Given
        User user = new User();
        user.setUsername("serhat");
        user.setEmail("serhat@test.com");
        user.setAuthorities(Set.of(Role.ROLE_USER));

        // When
        CreateUserResponse dto = userMapper.toDto(user, "Created");

        // Then
        assertThat(dto.username()).isEqualTo("serhat");
        assertThat(dto.email()).isEqualTo("serhat@test.com");

        assertThat(dto.role()).isEqualTo("ROLE_USER");

        assertThat(dto.message()).isEqualTo("Created");

        assertThat(dto.success()).isTrue();
    }

    // ----------------------------------------------------------
    // toDto (NULL User)
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDto -> User null ise error DTO dönmeli")
    void toDto_NullUser() {
        CreateUserResponse dto = userMapper.toDto(null, "Any message");

        assertThat(dto.success()).isFalse();
        assertThat(dto.message()).isEqualTo("User is null");
    }

    // ----------------------------------------------------------
    // toDto (EMPTY ROLES)
    // ----------------------------------------------------------
    @Test
    @DisplayName("toDto -> Rol listesi boşsa NO_ROLE dönmeli")
    void toDto_NoRoles() {
        User user = new User();
        user.setUsername("serhat");
        user.setEmail("test@test.com");
        user.setAuthorities(Set.of()); // boş roller

        CreateUserResponse dto = userMapper.toDto(user, "OK");

        assertThat(dto.role()).isEqualTo("NO_ROLE");
    }

    // ----------------------------------------------------------
    // toError()
    // ----------------------------------------------------------
    @Test
    @DisplayName("toError -> Error DTO üretmeli")
    void toError_ShouldReturnErrorDto() {
        CreateUserResponse dto = userMapper.toError("Hata oluştu");

        // DÜZELTME: status -> success (false)
        assertThat(dto.success()).isFalse();
        assertThat(dto.message()).isEqualTo("Hata oluştu");
    }

    // ----------------------------------------------------------
    // toEntity()
    // ----------------------------------------------------------
    @Test
    @DisplayName("toEntity -> DTO bilgileri doğru şekilde User entity'ye dönmeli")
    void toEntity_Success() {
        User user = userMapper.toEntity(
                "serhat",
                "serhat@test.com",
                "1234",
                Role.ROLE_USER
        );

        assertThat(user.getUsername()).isEqualTo("serhat");
        assertThat(user.getEmail()).isEqualTo("serhat@test.com");
        assertThat(user.getPassword()).isEqualTo("1234");

        // Roller: Role.ROLE_USER veriyoruz
        assertThat(user.getAuthorities())
                .containsExactly(Role.ROLE_USER);

        // Account flags
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
    }

    // ----------------------------------------------------------
    // updateEntity()
    // ----------------------------------------------------------
    @Test
    @DisplayName("updateEntity -> Var olan kullanıcı bilgileri güncellenmeli")
    void updateEntity_Success() {
        User user = new User();
        user.setEmail("old@test.com");
        user.setPassword("oldpass");
        user.setAuthorities(Set.of(Role.ROLE_ADMIN));

        Set<Role> newRoles = Set.of(Role.ROLE_USER);

        userMapper.updateEntity(user, "new@test.com", "newpass", newRoles);

        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.getPassword()).isEqualTo("newpass");
        assertThat(user.getAuthorities()).containsExactly(Role.ROLE_USER);
    }
}