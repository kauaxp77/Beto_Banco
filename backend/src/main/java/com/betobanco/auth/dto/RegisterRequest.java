package com.betobanco.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Informe o e-mail")
        @Email(message = "E-mail inválido")
        String email,

        @NotBlank(message = "Informe a senha")
        @Size(min = 8, message = "A senha precisa ter ao menos 8 caracteres")
        String password,

        @NotBlank(message = "Informe o nome completo")
        String fullName) {
}
