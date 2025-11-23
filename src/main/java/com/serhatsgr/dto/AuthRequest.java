package com.serhatsgr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(

        @NotBlank(message = "{login.password.notBlank}" )
        @Size(min=3, max=32, message="{login.username.size}")
        @Pattern(regexp="^[a-zA-Z0-9._-]+$", message="{login.username.pattern}")
        String username,

        @NotBlank
        @Size(min=12, max=128, message="{login.password.size}")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{12,128}$", message="{login.password.complexity}")
        String password
) {
}
