package com.serhatsgr.dto;

public record TokenPairDto(
        String accessToken,
        String refreshToken
) {
}
