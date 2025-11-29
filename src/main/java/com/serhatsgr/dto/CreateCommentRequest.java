package com.serhatsgr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Yorum içeriği boş olamaz")
        @Size(min = 1, max = 500, message = "Yorum içeriği 1 ile 500 karakter arasında olmalıdır")
        String content,

        @NotNull(message = "Film ID boş olamaz")
        Long filmId,

        Long parentCommentId
) {}
