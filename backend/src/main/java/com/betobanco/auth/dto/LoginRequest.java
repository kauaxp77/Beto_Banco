package com.betobanco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Informe a senha")
        String password) {
}
