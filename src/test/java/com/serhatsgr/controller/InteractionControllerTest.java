package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.InteractionController;
import com.serhatsgr.dto.DtoFilm;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.Impl.FavoriteService;
import com.serhatsgr.service.Impl.WatchedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.core.MethodParameter;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class InteractionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FavoriteService favoriteService;

    @Mock
    private WatchedService watchedService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private InteractionController interactionController;

    @BeforeEach
    void setup() {
        // UserDetails Resolver (Controller kullanmasa bile altyapı için hazır dursun)
        HandlerMethodArgumentResolver mockUserDetailsResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(UserDetails.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return new User("testUser", "password", Collections.emptyList());
            }
        };

        this.mockMvc = MockMvcBuilders.standaloneSetup(interactionController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .setCustomArgumentResolvers(mockUserDetailsResolver)
                .build();
    }

    // ==========================================================
    // FAVORITES TESTS
    // ==========================================================

    @Test
    @DisplayName("GET /favorites -> Favori listesini getir")
    void getFavorites_Success() throws Exception {
        // Arrange
        DtoFilm film = new DtoFilm();
        film.setTitle("Inception");

        // Service metodunun parametre alıp almadığını kontrol ettim (Controller'da parametresiz çağrılıyor)
        given(favoriteService.getMyFavorites()).willReturn(List.of(film));

        // Act & Assert
        mockMvc.perform(get("/rest/api/interactions/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Inception"));
    }

    @Test
    @DisplayName("POST /favorites/{filmId} -> Favoriye ekle")
    void addFavorite_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/rest/api/interactions/favorites/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Added"))
                .andExpect(jsonPath("$.message").value("Favorilere eklendi"));

        // Verify
        verify(favoriteService).addFavorite(1L);
    }

    @Test
    @DisplayName("POST /favorites/{filmId} -> Zaten ekliyse hata (Error Case)")
    void addFavorite_DuplicateError() throws Exception {
        // Arrange
        doThrow(new BaseException(new ErrorMessage(MessageType.DUPLICATE_RESOURCE, "Zaten favorilerde")))
                .when(favoriteService).addFavorite(1L);

        // Act & Assert
        mockMvc.perform(post("/rest/api/interactions/favorites/1"))
                .andExpect(status().is4xxClientError()) // Conflict veya Bad Request
                .andExpect(jsonPath("$.message").value("Zaten favorilerde"));
    }

    @Test
    @DisplayName("DELETE /favorites/{filmId} -> Favoriden çıkar")
    void removeFavorite_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/rest/api/interactions/favorites/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Removed"));

        // Verify
        verify(favoriteService).removeFavorite(1L);
    }

    // ==========================================================
    // WATCHED TESTS
    // ==========================================================

    @Test
    @DisplayName("GET /watched -> İzlenenleri getir")
    void getWatched_Success() throws Exception {
        // Arrange
        DtoFilm film = new DtoFilm();
        film.setTitle("Matrix");

        given(watchedService.getMyWatchedList()).willReturn(List.of(film));

        // Act & Assert
        mockMvc.perform(get("/rest/api/interactions/watched"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].title").value("Matrix"));
    }

    @Test
    @DisplayName("POST /watched/{filmId} -> İzledim olarak işaretle")
    void markWatched_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/rest/api/interactions/watched/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Marked"));

        // Verify
        verify(watchedService).markAsWatched(1L);
    }

    @Test
    @DisplayName("DELETE /watched/{filmId} -> İzledim işaretini kaldır")
    void unmarkWatched_Success() throws Exception {
        // Act & Assert
        mockMvc.perform(delete("/rest/api/interactions/watched/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Unmarked"));

        // Verify
        verify(watchedService).unmarkWatched(1L);
    }

    @Test
    @DisplayName("POST /watched/{filmId} -> Film bulunamadı hatası (Error Case)")
    void markWatched_NotFound() throws Exception {
        // Arrange
        doThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Film bulunamadı")))
                .when(watchedService).markAsWatched(99L);

        // Act & Assert
        mockMvc.perform(post("/rest/api/interactions/watched/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Film bulunamadı"));
    }
}