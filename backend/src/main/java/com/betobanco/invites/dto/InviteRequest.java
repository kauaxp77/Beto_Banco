package com.betobanco.invites.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InviteRequest(
        @NotBlank @Email String email,
        String fullName,
        @NotNull UUID productId,
        /** Nulo ou zero = acesso vitalicio. */
        @Min(0) @Max(3650) Integer validadeDias) {
}
