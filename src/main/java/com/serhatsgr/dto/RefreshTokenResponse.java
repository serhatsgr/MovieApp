package com.serhatsgr.dto;

public record RefreshTokenResponse(
        String refreshToken,
        String message
) {
    public static RefreshTokenResponse success( String refreshToken, String message) {
        return new RefreshTokenResponse( refreshToken, message);
    }

    public static RefreshTokenResponse error(String message) {
        return new RefreshTokenResponse(null,  message);
    }

}
