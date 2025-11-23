package com.serhatsgr.controller.Impl;

import com.serhatsgr.dto.*;
import com.serhatsgr.exception.BaseException;
import com.serhatsgr.exception.ErrorMessage;
import com.serhatsgr.exception.MessageType;
import com.serhatsgr.service.ICommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/rest/api/comments")
@Slf4j
@RequiredArgsConstructor
public class CommentControllerImpl {

    private final ICommentService commentService;

    // ===== Create Comment =====
    @PostMapping("/save")
    public ResponseEntity<ApiSuccess<CommentResponse>> createComment(
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            CommentResponse response = commentService.createComment(request);
            log.info("Yorum oluşturuldu. Kullanıcı: {}, Film ID: {}", userDetails.getUsername(), request.filmId());
            return ResponseEntity.ok(ApiSuccess.of("Yorum başarıyla oluşturuldu", response));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Yorum oluşturulurken hata: {}", e.getMessage(), e);
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Yorum oluşturulurken hata oluştu"));
        }
    }

    // ===== Update Comment =====
    @PutMapping("/update/{id}")
    public ResponseEntity<ApiSuccess<CommentResponse>> updateComment(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody UpdateCommentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            CommentResponse response = commentService.updateComment(id, request, userDetails.getUsername());
            log.info("Yorum güncellendi. Kullanıcı: {}, Yorum ID: {}", userDetails.getUsername(), id);
            return ResponseEntity.ok(ApiSuccess.of("Yorum başarıyla güncellendi", response));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Yorum güncellenirken hata: {}", e.getMessage(), e);
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Yorum güncellenirken hata oluştu"));
        }
    }

    // ===== Delete Comment =====
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<ApiSuccess<Void>> deleteComment(
            @PathVariable(name = "id") Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            commentService.deleteComment(id, userDetails.getUsername());
            log.info("Yorum silindi. Kullanıcı: {}, Yorum ID: {}", userDetails.getUsername(), id);
            return ResponseEntity.ok(ApiSuccess.of("Yorum başarıyla silindi", null));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Yorum silinirken hata: {}", e.getMessage(), e);
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Yorum silinirken hata oluştu"));
        }
    }

    // ===== Get Comments by Film =====
    @GetMapping("/film/{filmId}")
    public ResponseEntity<ApiSuccess<List<CommentResponse>>> getCommentsByFilm(
            @PathVariable Long filmId
    ) {
        try {
            List<CommentResponse> comments = commentService.getCommentsByFilm(filmId);
            log.info("Film ID: {} için yorumlar listelendi", filmId);
            return ResponseEntity.ok(ApiSuccess.of("Yorumlar başarıyla getirildi", comments));
        } catch (BaseException e) {
            throw e;
        } catch (Exception e) {
            log.error("Yorumlar getirilirken hata: {}", e.getMessage(), e);
            throw new BaseException(new ErrorMessage(MessageType.INTERNAL_ERROR, "Yorumlar getirilirken hata oluştu"));
        }
    }
}
