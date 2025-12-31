package com.serhatsgr.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serhatsgr.controller.Impl.CommentControllerImpl;
import com.serhatsgr.dto.CommentResponse;
import com.serhatsgr.dto.CreateCommentRequest;
import com.serhatsgr.dto.UpdateCommentRequest;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import org.springframework.context.MessageSource;
import com.serhatsgr.handler.GlobalExceptionHandler;
import com.serhatsgr.service.ICommentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ICommentService commentService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private CommentControllerImpl commentController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setup() {
        // --- BU KISIM SAYESİNDE BACKEND KODUNU DEĞİŞTİRMEDEN TEST ÇALIŞIR ---
        // Controller UserDetails istediğinde, biz ona manuel olarak dolu bir User nesnesi veriyoruz.
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

        // MockMvc'yi "Standalone" modda kuruyoruz.
        this.mockMvc = MockMvcBuilders.standaloneSetup(commentController)
                .setControllerAdvice(new GlobalExceptionHandler(messageSource)) // Exception Handler'ı ekle
                .setCustomArgumentResolvers(mockUserDetailsResolver) // Özel Resolver'ı ekle
                .build();
    }

    // ==========================================================
    // CREATE COMMENT
    // ==========================================================

    @Test
    @DisplayName("POST /save -> Yorum ekle (SUCCESS)")
    void createComment_Success() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("Harika film!", 1L, null);

        CommentResponse res = new CommentResponse(
                1L, "Harika film!", null, null, "testUser",
                1L, false, false, null, null);

        given(commentService.createComment(any(CreateCommentRequest.class))).willReturn(res);

        mockMvc.perform(post("/rest/api/comments/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Harika film!"));
    }

    @Test
    @DisplayName("POST /save -> Film bulunamazsa 404 dönmeli (ERROR CASE)")
    void createComment_BaseException() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("x", 1L, null);

        given(commentService.createComment(any(CreateCommentRequest.class)))
                .willThrow(new BaseException(new ErrorMessage(MessageType.NOT_FOUND, "Film bulunamadı")));

        mockMvc.perform(post("/rest/api/comments/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Film bulunamadı"));
    }

    @Test
    @DisplayName("POST /save -> INTERNAL ERROR")
    void createComment_InternalError() throws Exception {
        CreateCommentRequest req = new CreateCommentRequest("x", 1L, null);

        given(commentService.createComment(any(CreateCommentRequest.class)))
                .willThrow(new RuntimeException("DB error"));

        mockMvc.perform(post("/rest/api/comments/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isInternalServerError());
    }

    // ==========================================================
    // UPDATE COMMENT
    // ==========================================================

    @Test
    @DisplayName("PUT /update/{id} -> Yorum güncelle (SUCCESS)")
    void updateComment_Success() throws Exception {
        UpdateCommentRequest req = new UpdateCommentRequest("Yeni içerik");

        CommentResponse res = new CommentResponse(
                1L, "Yeni içerik", null, null, "testUser",
                1L, false, false, null, null);

        given(commentService.updateComment(eq(1L), any(UpdateCommentRequest.class), eq("testUser"))).willReturn(res);

        mockMvc.perform(put("/rest/api/comments/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Yeni içerik"));
    }

    @Test
    @DisplayName("PUT /update/{id} -> Yetkisiz işlem (ERROR CASE)")
    void updateComment_BaseException() throws Exception {
        UpdateCommentRequest req = new UpdateCommentRequest("Yeni içerik");

        given(commentService.updateComment(eq(1L), any(), eq("testUser")))
                .willThrow(new BaseException(
                        new ErrorMessage(MessageType.UNAUTHORIZED, "Yorumu güncelleyemezsiniz")
                ));

        mockMvc.perform(put("/rest/api/comments/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PUT /update/{id} -> INTERNAL ERROR")
    void updateComment_InternalError() throws Exception {
        UpdateCommentRequest req = new UpdateCommentRequest("Yeni içerik");

        given(commentService.updateComment(eq(1L), any(), eq("testUser")))
                .willThrow(new RuntimeException("DB ERROR"));

        mockMvc.perform(put("/rest/api/comments/update/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().is5xxServerError());
    }

    // ==========================================================
    // DELETE COMMENT
    // ==========================================================

    @Test
    @DisplayName("DELETE /delete/{id} -> Yorum sil (SUCCESS)")
    void deleteComment_Success() throws Exception {
        mockMvc.perform(delete("/rest/api/comments/delete/1"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /delete/{id} -> Yorum silinemez (ERROR CASE)")
    void deleteComment_BaseException() throws Exception {
        doThrow(new BaseException(new ErrorMessage(MessageType.FORBIDDEN, "Yorum silinemez")))
                .when(commentService).deleteComment(1L, "testUser");

        mockMvc.perform(delete("/rest/api/comments/delete/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Yorum silinemez"));
    }

    @Test
    @DisplayName("DELETE /delete/{id} -> INTERNAL ERROR")
    void deleteComment_InternalError() throws Exception {
        doThrow(new RuntimeException("DB ERROR"))
                .when(commentService).deleteComment(1L, "testUser");

        mockMvc.perform(delete("/rest/api/comments/delete/1"))
                .andExpect(status().is5xxServerError());
    }

    // ==========================================================
    // GET COMMENTS BY FILM
    // ==========================================================

    @Test
    @DisplayName("GET /film/{filmId} -> Yorumları getir (SUCCESS)")
    void getCommentsByFilm_Success() throws Exception {
        CommentResponse res = new CommentResponse(
                1L, "Nice", null, null, "user",
                1L, false, false, null, null
        );

        given(commentService.getCommentsByFilm(1L)).willReturn(List.of(res));

        mockMvc.perform(get("/rest/api/comments/film/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("GET /film/{filmId} -> Film bulunamadı (ERROR CASE)")
    void getCommentsByFilm_BaseException() throws Exception {
        given(commentService.getCommentsByFilm(1L))
                .willThrow(new BaseException(
                        new ErrorMessage(MessageType.NOT_FOUND, "Film bulunamadı")
                ));

        mockMvc.perform(get("/rest/api/comments/film/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /film/{filmId} -> INTERNAL ERROR")
    void getCommentsByFilm_InternalError() throws Exception {
        given(commentService.getCommentsByFilm(1L))
                .willThrow(new RuntimeException("DB ERROR"));

        mockMvc.perform(get("/rest/api/comments/film/1"))
                .andExpect(status().is5xxServerError());
    }
}