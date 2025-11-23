package com.serhatsgr.dto;

public record CreateUserResponse(
        String username,
        String email,
        String role,
        String message,
        boolean success
) {

    public static CreateUserResponse success(String username, String email, String role, String message) {
        return new CreateUserResponse(username, email, role, message, true);
    }

    public static CreateUserResponse error(String message) {
        return new CreateUserResponse(null, null, null, message, false);
    }

}
