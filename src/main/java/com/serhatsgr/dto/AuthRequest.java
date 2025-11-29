package com.serhatsgr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AuthRequest(

        @NotBlank(message = "Kullanıcı adınızı giriniz" )
        String username,

        @NotBlank (message = "Lütfen şifrenizi giriniz" )
        String password
) {
}
