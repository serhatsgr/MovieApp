package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.RatingController;
import com.serhatsgr.dto.RatingRequest;
import com.serhatsgr.dto.UserRatingResponse;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.Impl.RatingService;
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

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RatingControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RatingService ratingService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private RatingController ratingController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // --- UserDetails Otomatik Doldurucu ---
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

        this.mockMvc = MockMvcBuilders.standaloneSetup(ratingController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .setCustomArgumentResolvers(mockUserDetailsResolver)
                .build();
    }

    // ==========================================================
    // RATE MOVIE (ADD / UPDATE)
    // URL: POST /rest/api/movies/{movieId}/ratings
    // ==========================================================

    @Test
    @DisplayName("POST /rate -> Puan ver (SUCCESS)")
    void rateMovie_Success() throws Exception {
        RatingRequest req = new RatingRequest(4);

        mockMvc.perform(post("/rest/api/movies/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Oylama başarılı"));

        verify(ratingService).createOrUpdateRating(eq(1L), any(RatingRequest.class));
    }

    @Test
    @DisplayName("POST /rate -> Geçersiz puan (VALIDATION ERROR)")
    void rateMovie_InvalidScore() throws Exception {
        RatingRequest req = new RatingRequest(99); // max 10 olduğunu varsayıyoruz

        mockMvc.perform(post("/rest/api/movies/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /rate -> Yetkisiz işlem (ERROR CASE)")
    void rateMovie_BaseException() throws Exception {
        RatingRequest req = new RatingRequest(5);

        doThrow(new BaseException(new ErrorMessage(MessageType.UNAUTHORIZED, "Oylama yapılamaz")))
                .when(ratingService).createOrUpdateRating(eq(1L), any(RatingRequest.class));

        mockMvc.perform(post("/rest/api/movies/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Oylama yapılamaz"));
    }

    @Test
    @DisplayName("POST /rate -> Sunucu hatası (INTERNAL ERROR)")
    void rateMovie_InternalError() throws Exception {
        RatingRequest req = new RatingRequest(4);

        doThrow(new RuntimeException("DB ERROR"))
                .when(ratingService).createOrUpdateRating(eq(1L), any(RatingRequest.class));

        mockMvc.perform(post("/rest/api/movies/1/ratings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    // ==========================================================
    // GET USER RATING
    // URL: GET /rest/api/movies/{movieId}/ratings/me
    // ==========================================================

    @Test
    @DisplayName("GET /me -> Kullanıcı puanını getir (SUCCESS)")
    void getUserRating_Success() throws Exception {
        UserRatingResponse response = new UserRatingResponse(9);

        given(ratingService.getUserRating(eq(1L))).willReturn(response);

        mockMvc.perform(get("/rest/api/movies/1/ratings/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.score").value(9));
    }

    @Test
    @DisplayName("GET /me -> Puan yoksa hata (ERROR CASE)")
    void getUserRating_NotFound() throws Exception {
        given(ratingService.getUserRating(1L))
                .willThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Puan bulunamadı")));

        mockMvc.perform(get("/rest/api/movies/1/ratings/me"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Puan bulunamadı"));
    }

    @Test
    @DisplayName("GET /me -> Sunucu hatası (INTERNAL ERROR)")
    void getUserRating_InternalError() throws Exception {
        given(ratingService.getUserRating(1L))
                .willThrow(new RuntimeException("Unexpected"));

        mockMvc.perform(get("/rest/api/movies/1/ratings/me"))
                .andExpect(status().isInternalServerError());
    }

    // ==========================================================
    // DELETE RATING
    // URL: DELETE /rest/api/movies/{movieId}/ratings
    // ==========================================================

    @Test
    @DisplayName("DELETE -> Puanı sil (SUCCESS)")
    void deleteRating_Success() throws Exception {
        mockMvc.perform(delete("/rest/api/movies/1/ratings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Oylama silindi"));

        verify(ratingService).deleteRating(1L);
    }

    @Test
    @DisplayName("DELETE -> Puan bulunamadı (ERROR CASE)")
    void deleteRating_BaseException() throws Exception {
        doThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Puan bulunamadı")))
                .when(ratingService).deleteRating(10L);

        mockMvc.perform(delete("/rest/api/movies/10/ratings"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Puan bulunamadı"));
    }

    @Test
    @DisplayName("DELETE -> Sunucu hatası (INTERNAL ERROR)")
    void deleteRating_InternalError() throws Exception {
        doThrow(new RuntimeException("DB FAIL"))
                .when(ratingService).deleteRating(5L);

        mockMvc.perform(delete("/rest/api/movies/5/ratings"))
                .andExpect(status().isInternalServerError());
    }
}