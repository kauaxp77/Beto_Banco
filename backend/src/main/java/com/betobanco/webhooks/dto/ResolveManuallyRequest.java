package com.betobanco.webhooks.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Formulario da resolucao manual: para quem liberar, e o que (spec 7.4). */
public record ResolveManuallyRequest(
        @NotBlank @Email String email,
        @NotNull UUID productId) {
}
