package com.serhatsgr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Yorum içeriği boş olamaz")
        @Size(min = 1, max = 500, message = "Yorum içeriği 1 ile 500 karakter arasında olmalıdır")
        String content
) {}
