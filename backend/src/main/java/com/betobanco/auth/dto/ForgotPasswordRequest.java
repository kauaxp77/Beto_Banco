package com.betobanco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        String email) {
}
