package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.UserProfileController;
import com.serhatsgr.dto.TokenPairDto;
import com.serhatsgr.dto.UpdateUserRequest;
import com.serhatsgr.entity.Role;
import com.serhatsgr.entity.User;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.Impl.JwtService;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private UserProfileController userProfileController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // --- Custom Argument Resolver ---
        // Controller ne zaman @AuthenticationPrincipal UserDetails istese,
        // ona username="testUser" olan sahte bir kullanıcı veriyoruz.
        HandlerMethodArgumentResolver mockUserDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new org.springframework.security.core.userdetails.User(
                        "testUser", "password", Collections.emptyList());
            }
        };

        this.mockMvc = MockMvcBuilders.standaloneSetup(userProfileController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource)) // Hata yönetimi
                .setCustomArgumentResolvers(mockUserDetailsResolver) // Kullanıcı enjeksiyonu
                .build();
    }

    // ==========================================================
    // UPDATE USER PROFILE
    // ==========================================================

    @Test
    @DisplayName("PUT /update -> Profil güncelleme (SUCCESS)")
    void updateUser_Success() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest("newUsername", "new@mail.com");

        User updatedUser = new User();
        updatedUser.setUsername("newUsername");
        // Controller içinde role çekildiği için authorities set edilmeli
        updatedUser.setAuthorities(Set.of(Role.ROLE_USER));

        TokenPairDto tokenPair = new TokenPairDto("newAccess", "newRefresh");

        // Mock: UserService güncelleme sonrası user döner
        given(userService.updateUser(eq("testUser"), any(UpdateUserRequest.class)))
                .willReturn(updatedUser);

        // Mock: JwtService yeni token üretir
        given(jwtService.generateTokenPair("newUsername"))
                .willReturn(tokenPair);

        // Act & Assert
        mockMvc.perform(put("/rest/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Güncelleme başarılı"))
                .andExpect(jsonPath("$.data.username").value("newUsername"))
                .andExpect(jsonPath("$.data.accessToken").value("newAccess"));
    }

    @Test
    @DisplayName("PUT /update -> Kullanıcı adı zaten alınmış (ERROR CASE)")
    void updateUser_DuplicateUsername() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest("takenUser", "valid@mail.com");

        given(userService.updateUser(eq("testUser"), any(UpdateUserRequest.class)))
                .willThrow(new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Kullanıcı adı kullanımda")));

        // Act & Assert
        mockMvc.perform(put("/rest/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError()) // Conflict (409) veya Bad Request (400)
                .andExpect(jsonPath("$.message").value("Kullanıcı adı kullanımda"));
    }

    @Test
    @DisplayName("PUT /update -> Sunucu hatası (INTERNAL ERROR)")
    void updateUser_InternalError() throws Exception {
        // Arrange
        UpdateUserRequest request = new UpdateUserRequest("user", "mail@mail.com");

        given(userService.updateUser(eq("testUser"), any(UpdateUserRequest.class)))
                .willThrow(new RuntimeException("DB Connection Failed"));

        // Act & Assert
        mockMvc.perform(put("/rest/api/user/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ==========================================================
    // DELETE ACCOUNT
    // ==========================================================

    @Test
    @DisplayName("DELETE /delete -> Hesabı sil (SUCCESS)")
    void deleteAccount_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/rest/api/user/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hesabınız başarıyla silindi. Güle güle!"));

        // Verify: Service o anki kullanıcı adıyla ("testUser") çağrıldı mı?
        verify(userService).deleteUserByUsername("testUser");
    }

    @Test
    @DisplayName("DELETE /delete -> Kullanıcı bulunamadı (ERROR CASE)")
    void deleteAccount_NotFound() throws Exception {
        // Arrange
        doThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Kullanıcı bulunamadı")))
                .when(userService).deleteUserByUsername("testUser");

        // Act & Assert
        mockMvc.perform(delete("/rest/api/user/delete"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Kullanıcı bulunamadı"));
    }
}