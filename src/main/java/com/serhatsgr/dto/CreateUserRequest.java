package com.serhatsgr.dto;

import com.serhatsgr.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Set;

@Builder
public record CreateUserRequest(

        @NotBlank(message = "{user.username.notblank}")
        @Size(min = 3, max = 32, message = "{user.username.size}")
        String username,

        @NotBlank(message = "{user.email.notblank}")
        @Email(message = "{user.email.invalid}")
        String email,

        @NotBlank(message = "{user.password.notblank}")
        @Size(min = 12, max = 128, message = "{user.password.size}")
        String password,

        @NotNull(message = "{user.authorities.notnull}")
        Set<Role> authorities

) {}

