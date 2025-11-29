package com.serhatsgr.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record UserDto(
        Long id,
        String username,
        String email,
        boolean isEnabled,
        Set<String> roles,
        LocalDateTime createdAt
) {}