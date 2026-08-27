package com.betobanco.catalog.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ProductCreateRequest(
        @NotBlank String sku,
        @NotBlank String name,
        String description,
        @Min(0) long priceCents) {
}
