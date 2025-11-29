package com.serhatsgr.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,

        @NotBlank(message = "{user.password.notblank}")
        @Size(min = 12, max = 128, message = "{user.password.size}")
        String newPassword,

        @NotBlank String confirmNewPassword
) {}