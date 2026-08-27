package com.betobanco.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StudentUpdateRequest(
        @NotBlank(message = "Informe o nome completo")
        @Size(max = 120, message = "Nome muito longo")
        String fullName,

        @Pattern(regexp = "^$|^[0-9]{10,13}$",
                message = "Telefone deve conter de 10 a 13 dígitos")
        String phone) {
}
