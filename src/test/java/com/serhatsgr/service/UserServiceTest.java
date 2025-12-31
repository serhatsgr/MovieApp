package com.serhatsgr.service;

import com.serhatsgr.dto.CreateUserRequest;
import com.serhatsgr.dto.CreateUserResponse;
import com.serhatsgr.dto.UpdateUserRequest;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.mapper.UserMapper;
import com.serhatsgr.repository.CommentRepository;
import com.serhatsgr.repository.UserRepository;
import com.serhatsgr.service.Impl.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserMapper userMapper;
    @Mock private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks private UserService userService;


    // -----------------------------------------------------------------------
    // CREATE USER TESTLERİ
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("createUser -> Başarılı senaryo")
    void createUser_Success() {
        CreateUserRequest request = new CreateUserRequest(
                "newUser", "mail@test.com", "pass", Set.of(Role.ROLE_USER)
        );

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newUser");

        given(userRepository.findByUsername("newUser")).willReturn(Optional.empty());
        given(userRepository.findByEmail("mail@test.com")).willReturn(Optional.empty());
        given(passwordEncoder.encode("pass")).willReturn("encodedPass");
        given(userRepository.save(any(User.class))).willReturn(savedUser);

        given(userMapper.toDto(any(User.class), anyString()))
                .willReturn(new CreateUserResponse("newUser", "mail@test.com", "ROLE_USER", "Success", true));

        CreateUserResponse response = userService.createUser(request);

        assertThat(response).isNotNull();
        assertThat(response.username()).isEqualTo("newUser");
        assertThat(response.success()).isTrue();
    }

    @Test
    @DisplayName("createUser -> Username çakışması")
    void createUser_DuplicateUsername() {
        CreateUserRequest request = new CreateUserRequest("existing", "mail@test.com", "pass", Set.of(Role.ROLE_USER));

        given(userRepository.findByUsername("existing")).willReturn(Optional.of(new User()));

        Throwable ex = catchThrowable(() -> userService.createUser(request));

        assertThat(ex).isInstanceOf(BaseException.class);
        assertThat(((BaseException) ex).getErrorMessage().getMessageType())
                .isEqualTo(MessageType.DUPLICATE_RESOURCE);
    }

    @Test
    @DisplayName("createUser -> Email çakışması")
    void createUser_DuplicateEmail() {
        CreateUserRequest request = new CreateUserRequest("user", "duplicate@mail.com", "pass", Set.of(Role.ROLE_USER));

        given(userRepository.findByUsername("user")).willReturn(Optional.empty());
        given(userRepository.findByEmail("duplicate@mail.com")).willReturn(Optional.of(new User()));

        Throwable ex = catchThrowable(() -> userService.createUser(request));

        assertThat(ex).isInstanceOf(BaseException.class);
        assertThat(((BaseException) ex).getErrorMessage().getMessageType())
                .isEqualTo(MessageType.DUPLICATE_RESOURCE);
    }


    // -----------------------------------------------------------------------
    // UPDATE USER TESTLERİ
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("updateUser -> Başarılı güncelleme")
    void updateUser_Success() {
        User existing = new User();
        existing.setUsername("oldName");
        existing.setEmail("old@mail.com");

        UpdateUserRequest request = new UpdateUserRequest("newName", "new@mail.com");

        given(userRepository.findByUsername("oldName")).willReturn(Optional.of(existing));
        given(userRepository.findByUsername("newName")).willReturn(Optional.empty());
        given(userRepository.save(existing)).willReturn(existing);

        User result = userService.updateUser("oldName", request);

        assertThat(result.getUsername()).isEqualTo("newName");
        assertThat(result.getEmail()).isEqualTo("new@mail.com");
    }

    @Test
    @DisplayName("updateUser -> Kullanıcı bulunamadı")
    void updateUser_UserNotFound() {
        given(userRepository.findByUsername("unknown")).willReturn(Optional.empty());

        Throwable ex = catchThrowable(() -> userService.updateUser("unknown", new UpdateUserRequest("a", "b")));

        assertThat(ex).isInstanceOf(BaseException.class);
        assertThat(((BaseException) ex).getErrorMessage().getMessageType())
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("updateUser -> Yeni username başka kullanıcıda kayıtlı")
    void updateUser_DuplicateUsername() {
        User existing = new User();
        existing.setUsername("oldName");

        given(userRepository.findByUsername("oldName")).willReturn(Optional.of(existing));
        given(userRepository.findByUsername("newName")).willReturn(Optional.of(new User()));

        Throwable ex = catchThrowable(() -> userService.updateUser(
                "oldName",
                new UpdateUserRequest("newName", "new@mail.com")
        ));

        assertThat(ex).isInstanceOf(BaseException.class);
        assertThat(((BaseException) ex).getErrorMessage().getMessageType())
                .isEqualTo(MessageType.DUPLICATE_RESOURCE);
    }


    // -----------------------------------------------------------------------
    // DELETE USER TESTLERİ
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("deleteUser -> Kullanıcı ve yorumları silinir")
    void deleteUser_Success() {
        given(userRepository.existsById(1L)).willReturn(true);

        userService.deleteUser(1L);

        verify(commentRepository).deleteByUserId(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteUser -> Kullanıcı bulunamadı hatası")
    void deleteUser_NotFound() {
        given(userRepository.existsById(999L)).willReturn(false);

        Throwable ex = catchThrowable(() -> userService.deleteUser(999L));

        assertThat(ex).isInstanceOf(BaseException.class);
        assertThat(((BaseException) ex).getErrorMessage().getMessageType())
                .isEqualTo(MessageType.RESOURCE_NOT_FOUND);
    }
}
