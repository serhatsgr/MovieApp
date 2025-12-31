package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.AdminUserController;
import com.serhatsgr.dto.UserDto;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.Impl.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private AdminUserController adminUserController;

    @BeforeEach
    void setup() {
        // UserDetails Resolver (Controller'da olmasa bile güvenlik filtreleri için standart yapı)
        HandlerMethodArgumentResolver mockUserDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new User("adminUser", "password", Collections.emptyList());
            }
        };

        this.mockMvc = MockMvcBuilders.standaloneSetup(adminUserController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .setCustomArgumentResolvers(mockUserDetailsResolver)
                .build();
    }

    // ==========================================================
    // GET ALL USERS
    // ==========================================================

    @Test
    @DisplayName("GET / -> Tüm kullanıcıları getir (SUCCESS)")
    void getAllUsers_Success() throws Exception {
        // Arrange
        UserDto user1 = new UserDto(1L, "john_doe", "john@example.com", true, Set.of("ROLE_USER"), LocalDateTime.now());
        UserDto user2 = new UserDto(2L, "jane_doe", "jane@example.com", false, Set.of("ROLE_ADMIN"), LocalDateTime.now());

        given(userService.getAllUsers()).willReturn(List.of(user1, user2));

        // Act & Assert
        mockMvc.perform(get("/rest/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].username").value("john_doe"));
    }

    // ==========================================================
    // GET USER BY ID
    // ==========================================================

    @Test
    @DisplayName("GET /{id} -> Kullanıcı detayını getir (SUCCESS)")
    void getUserById_Success() throws Exception {
        UserDto user = new UserDto(10L, "example_user", "user@example.com", true, Set.of("ROLE_USER"), LocalDateTime.now());

        given(userService.getUserById(10L)).willReturn(user);

        mockMvc.perform(get("/rest/api/admin/users/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("example_user"));
    }

    @Test
    @DisplayName("GET /{id} -> Kullanıcı bulunamadı (ERROR CASE)")
    void getUserById_NotFound() throws Exception {
        given(userService.getUserById(55L))
                .willThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı")));

        mockMvc.perform(get("/rest/api/admin/users/55"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Kullanıcı bulunamadı"));
    }

    // ==========================================================
    // BAN / UNBAN USER
    // ==========================================================

    @Test
    @DisplayName("PUT /{id}/ban -> Kullanıcı ban durumu değiştir (SUCCESS)")
    void banUser_Success() throws Exception {
        UserDto updated = new UserDto(5L, "ban_test", "ban@test.com", false, Set.of("ROLE_USER"), LocalDateTime.now());

        given(userService.toggleUserBan(5L)).willReturn(updated);

        mockMvc.perform(put("/rest/api/admin/users/5/ban"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("ban_test"))
                .andExpect(jsonPath("$.message").value("Kullanıcı durumu güncellendi"));
    }

    @Test
    @DisplayName("PUT /{id}/ban -> Kullanıcı bulunamadı (ERROR CASE)")
    void banUser_NotFound() throws Exception {
        doThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı")))
                .when(userService).toggleUserBan(44L);

        mockMvc.perform(put("/rest/api/admin/users/44/ban"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Kullanıcı bulunamadı"));
    }

    // ==========================================================
    // CHANGE USER ROLE
    // ==========================================================

    @Test
    @DisplayName("PUT /{id}/role -> Kullanıcı rolü değiştirildi (SUCCESS)")
    void changeRole_Success() throws Exception {
        UserDto updated = new UserDto(8L, "role_user", "role@test.com", true, Set.of("ROLE_ADMIN"), LocalDateTime.now());

        given(userService.toggleUserRole(8L)).willReturn(updated);

        mockMvc.perform(put("/rest/api/admin/users/8/role"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles[0]").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.message").value("Kullanıcı rolü değiştirildi"));
    }

    @Test
    @DisplayName("PUT /{id}/role -> Kullanıcı bulunamadı (ERROR CASE)")
    void changeRole_NotFound() throws Exception {
        doThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı")))
                .when(userService).toggleUserRole(777L);

        mockMvc.perform(put("/rest/api/admin/users/777/role"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Kullanıcı bulunamadı"));
    }

    // ==========================================================
    // DELETE USER
    // ==========================================================

    @Test
    @DisplayName("DELETE /{id} -> Kullanıcı sil (SUCCESS)")
    void deleteUser_Success() throws Exception {
        mockMvc.perform(delete("/rest/api/admin/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Kullanıcı silindi"));

        // Assert
        verify(userService).deleteUser(1L);
    }

    @Test
    @DisplayName("DELETE /{id} -> Kullanıcı bulunamadı (ERROR CASE)")
    void deleteUser_NotFound() throws Exception {
        // Arrange
        doThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı")))
                .when(userService).deleteUser(99L);

        // Act & Assert
        mockMvc.perform(delete("/rest/api/admin/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Kullanıcı bulunamadı"));
    }
}