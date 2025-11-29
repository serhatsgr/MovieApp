package com.serhatsgr.dto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RatingRequest(
        @NotNull(message = "Puan boş olamaz")
        @Min(value = 1, message = "Puan en az 1 olmalıdır")
        @Max(value = 5, message = "Puan en fazla 5 olmalıdır")
        Integer score
) {}