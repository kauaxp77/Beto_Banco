package com.betobanco.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** O SKU nao muda: e a chave pela qual o gateway referencia o produto. */
public record ProductUpdateRequest(
        @NotBlank String name,
        String description,
        @Min(0) long priceCents,
        @NotNull Boolean active) {
}
