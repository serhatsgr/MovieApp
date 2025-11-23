package com.serhatsgr.controller;

import com.serhatsgr.dto.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface ICommentController {

    ResponseEntity<ApiSuccess<CommentResponse>> createComment(CreateCommentRequest request, String username);

    ResponseEntity<ApiSuccess<CommentResponse>> updateComment(Long id, UpdateCommentRequest request, String username);

    ResponseEntity<ApiSuccess<Void>> deleteComment(Long id, String username);

    ResponseEntity<ApiSuccess<List<CommentResponse>>> getCommentsByFilm(Long filmId);
}
