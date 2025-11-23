package com.serhatsgr.service;

import com.serhatsgr.dto.*;
import java.util.List;

public interface ICommentService {

    CommentResponse createComment(CreateCommentRequest request);

    CommentResponse updateComment(Long commentId, UpdateCommentRequest request, String username);

    void deleteComment(Long commentId, String username);

    List<CommentResponse> getCommentsByFilm(Long filmId);
}
