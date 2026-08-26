package com.betobanco.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token ausente")
        String token,

        @NotBlank(message = "Informe a senha")
        @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres")
        String password) {
}
