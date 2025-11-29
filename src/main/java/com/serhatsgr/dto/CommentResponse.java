package com.serhatsgr.dto;

import java.time.LocalDateTime;
import java.util.List;

public record CommentResponse(
        Long id,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String userName,
        Long filmId,
        boolean isAuthorBanned,
        boolean isDeleted,
        Long parentCommentId,
        List<CommentResponse> replies
) {}
