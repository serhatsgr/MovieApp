package com.serhatsgr.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String username,
        String role,
        String message,
        boolean success
) {
    public static AuthResponse success(String accessToken, String refreshToken, String username, String role, String message) {
        return new AuthResponse(accessToken, refreshToken, username, role, message, true);
    }

    public static AuthResponse error(String message) {
        return new AuthResponse(null, null, null, null, message, false);
    }
}